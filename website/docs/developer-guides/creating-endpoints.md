---
sidebar_position: 2
---

# Creating New Endpoints

This guide explains how to create new **protected** (authenticated) and **public** endpoints following our API-First approach. We'll walk through the complete flow: from OpenAPI specification to backend implementation to frontend consumption.

## Overview

Our architecture follows these principles:

1. **API-First Design** — The OpenAPI spec is the contract; code is generated from it
2. **BFF Pattern** — Gateway handles authentication; backend receives JWT tokens
3. **Security by Default** — Endpoints require authentication unless explicitly marked public

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   OpenAPI Spec  │ ──► │ Backend (Java)  │ ──► │ Frontend (React)│
│  /api/spec      │     │ Implements API  │     │ Consumes API    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## Part 1: API-First — Define the Contract

All endpoints start in the OpenAPI specification. This ensures frontend and backend are always aligned.

### Location

```
api/specification/openapi.yaml
```

### Creating a Public Endpoint

Public endpoints are accessible without authentication. Use `security: []` to mark them as public.

**Example: List products (public)**

```yaml
paths:
  /v1/products:
    get:
      tags:
        - Products
      summary: List all products
      description: Returns a paginated list of products. No authentication required.
      operationId: listProducts
      security: []  # 👈 Public endpoint - no authentication required
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
      responses:
        '200':
          description: A list of products
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductPage'
        '400':
          $ref: '#/components/responses/BadRequest'
```

**Key points:**
- `security: []` explicitly marks the endpoint as public
- Use existing parameter references (`PageParam`, `SizeParam`) for consistency
- Reference shared response schemas for error handling

### Creating a Protected Endpoint

Protected endpoints require a valid JWT token. Reference the `BearerAuth` security scheme.

**Example: Create a product (authenticated)**

```yaml
paths:
  /v1/products:
    post:
      tags:
        - Products
      summary: Create a new product
      description: Creates a product. Requires authentication.
      operationId: createProduct
      security:
        - BearerAuth: []  # 👈 Requires authentication
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateProductRequest'
      responses:
        '201':
          description: Product created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'   # 👈 Always include for protected endpoints
        '403':
          $ref: '#/components/responses/Forbidden'      # 👈 For authorization failures
```

**Key points:**
- `security: - BearerAuth: []` requires a valid JWT token
- Always include `401` and `403` error responses for protected endpoints
- The `BearerAuth` scheme is defined in `components/securitySchemes`

### Define Request/Response Schemas

Add your schemas in the `components/schemas` section:

```yaml
components:
  schemas:
    CreateProductRequest:
      type: object
      additionalProperties: false
      properties:
        name:
          type: string
          description: Product name
          minLength: 1
          maxLength: 255
          example: "Widget Pro"
        price:
          type: number
          format: double
          minimum: 0
          description: Product price in dollars
          example: 29.99
      required: [name, price]

    ProductResponse:
      type: object
      additionalProperties: false
      properties:
        id:
          type: string
          description: Unique identifier (TSID)
          example: "506979954615549952"
        name:
          type: string
          description: Product name
        price:
          type: number
          format: double
          description: Product price
        createdAt:
          type: string
          format: date-time
          description: Creation timestamp
      required: [id, name, price, createdAt]
```

### Generate Code

After updating the OpenAPI spec, regenerate the client code:

```bash
# Backend (Java) - generates interfaces and DTOs
cd backend
./mvnw generate-sources

# Frontend (TypeScript) - generates type-safe client
cd frontend
npm run generate:api
```

---

## Part 2: Backend — Implement the Contract

The backend implements the generated interfaces. Spring Addons handles JWT validation automatically.

### File Structure

```
backend/src/main/java/com/example/demo/
├── product/                          # Feature module
│   ├── controller/
│   │   └── ProductController.java    # Implements generated API interface
│   ├── service/
│   │   └── ProductService.java       # Business logic
│   ├── repository/
│   │   └── ProductRepository.java    # Data access
│   ├── mapper/
│   │   └── ProductMapper.java        # Entity ↔ DTO mapping
│   ├── model/
│   │   └── Product.java              # JPA entity
│   └── exception/
│       └── ProductNotFoundException.java
```

### Step 1: Create the Controller

The controller implements the generated `ProductsApi` interface:

```java
package com.example.demo.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.api.v1.controller.ProductsApi;
import com.example.demo.api.v1.model.CreateProductRequest;
import com.example.demo.api.v1.model.ProductPage;
import com.example.demo.api.v1.model.ProductResponse;
import com.example.demo.product.mapper.ProductMapper;
import com.example.demo.product.service.ProductService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductsApi {

    private final ProductService service;
    private final ProductMapper mapper;

    // PUBLIC ENDPOINT - No authentication required
    // Security is handled via Spring Addons config (permit-all pattern)
    @Override
    public ResponseEntity<ProductPage> listProducts(Integer page, Integer size) {
        var entityPage = service.getProducts(page, size);
        return ResponseEntity.ok(mapper.toProductPage(entityPage));
    }

    // PROTECTED ENDPOINT - JWT validated by Spring Addons
    // Gateway forwards JWT → Spring validates → method executes
    @Override
    public ResponseEntity<ProductResponse> createProduct(CreateProductRequest request) {
        var entity = service.createProduct(
            request.getName(),
            request.getPrice()
        );
        return ResponseEntity.status(201).body(mapper.toProductResponse(entity));
    }
}
```

**Important:** The controller does NOT handle authentication logic. Spring Addons automatically:
- Validates JWT tokens from the Gateway
- Extracts user roles from the token
- Rejects requests with invalid/expired tokens

### Step 2: Configure Security

Security is configured in `application-*.properties`. Public endpoints are specified in the `permit-all` property:

```properties
# backend/src/main/resources/application-local.properties

# Public endpoints (match your OpenAPI spec security: [] endpoints)
com.c4-soft.springaddons.oidc.resourceserver.permit-all=\
    /error,\
    /actuator/health/**,\
    /api/v1/greetings,\
    /api/v1/greetings/**,\
    /api/v1/products         # 👈 Add your new public GET endpoint
```

**Note:** If your endpoint needs to be public for GET but protected for POST (like `/api/v1/products`), you need custom security config:

```java
// backend/src/main/java/com/example/demo/common/config/WebSecurityConfig.java

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            // Public: GET products list
            .requestMatchers(HttpMethod.GET, "/api/v1/products").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/products/*").permitAll()
            // Protected: POST/PUT/DELETE products
            .requestMatchers(HttpMethod.POST, "/api/v1/products").authenticated()
            .requestMatchers(HttpMethod.PUT, "/api/v1/products/*").authenticated()
            .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*").authenticated()
            // Everything else requires authentication
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

### Step 3: Accessing the Authenticated User

In protected endpoints, you can access the authenticated user's information:

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Override
public ResponseEntity<ProductResponse> createProduct(
        CreateProductRequest request,
        Authentication auth  // 👈 Inject authentication
) {
    // Get username from JWT
    String username = auth.getName();

    // Get roles
    var roles = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

    // Access JWT claims directly
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
        String email = (String) jwtAuth.getTokenAttributes().get("email");
        String preferredUsername = (String) jwtAuth.getTokenAttributes().get("preferred_username");
    }

    var entity = service.createProduct(request.getName(), request.getPrice(), username);
    return ResponseEntity.status(201).body(mapper.toProductResponse(entity));
}
```

### Step 4: Role-Based Authorization

For fine-grained access control, use `@PreAuthorize`:

```java
import org.springframework.security.access.prepost.PreAuthorize;

// Only users with ADMIN role can delete products
@Override
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> deleteProduct(String id) {
    service.deleteProduct(Long.parseLong(id));
    return ResponseEntity.noContent().build();
}

// Users with either USER or ADMIN role can create products
@Override
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<ProductResponse> createProduct(CreateProductRequest request) {
    // ...
}
```

**Enable method security** in your config:

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
}
```

---

## Part 3: Frontend — Consume the API

The frontend uses the generated type-safe client to call endpoints.

### File Structure

```
frontend/src/features/
├── products/                         # Feature module
│   ├── components/
│   │   ├── ProductList.tsx
│   │   └── CreateProductForm.tsx
│   ├── hooks/
│   │   ├── index.ts
│   │   ├── useProducts.ts           # Hook for listing (public)
│   │   └── useProductMutations.ts   # Hook for create/update/delete (protected)
│   └── index.ts
```

### Step 1: Create Hook for Public Endpoint

Public endpoints don't need authentication headers:

```typescript
// frontend/src/features/products/hooks/useProducts.ts

import { useState, useEffect, useCallback } from "react";
import { listProducts } from "../../../api/config";
import { parseApiError, type ApiError } from "../../../api/errors";
import type { ProductResponse, PageMeta } from "../../../api/generated";

export interface UseProductsResult {
    products: ProductResponse[];
    meta: PageMeta | null;
    loading: boolean;
    error: ApiError | null;
    refresh: () => Promise<void>;
}

export function useProducts(page = 0, size = 20): UseProductsResult {
    const [products, setProducts] = useState<ProductResponse[]>([]);
    const [meta, setMeta] = useState<PageMeta | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<ApiError | null>(null);

    const fetchProducts = useCallback(async () => {
        setLoading(true);
        setError(null);

        try {
            // Public endpoint - no auth needed
            const response = await listProducts({ query: { page, size } });

            if (response.data) {
                setProducts(response.data.data ?? []);
                setMeta(response.data.meta ?? null);
            }
        } catch (err) {
            setError(parseApiError(err));
        } finally {
            setLoading(false);
        }
    }, [page, size]);

    useEffect(() => {
        fetchProducts();
    }, [fetchProducts]);

    return { products, meta, loading, error, refresh: fetchProducts };
}
```

### Step 2: Create Hook for Protected Endpoint

Protected endpoints use the `useAuth` hook to check authentication status. The Gateway automatically attaches the session cookie, which is translated to a JWT:

```typescript
// frontend/src/features/products/hooks/useProductMutations.ts

import { useState, useCallback } from "react";
import { createProduct } from "../../../api/config";
import { parseApiError, type ApiError } from "../../../api/errors";
import { useAuth } from "../../auth/hooks";
import type { CreateProductRequest, ProductResponse } from "../../../api/generated";

export interface UseProductMutationsResult {
    create: (data: CreateProductRequest) => Promise<ProductResponse | null>;
    loading: boolean;
    error: ApiError | null;
}

export function useProductMutations(): UseProductMutationsResult {
    const { isAuthenticated, login } = useAuth();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ApiError | null>(null);

    const create = useCallback(async (data: CreateProductRequest): Promise<ProductResponse | null> => {
        // Redirect to login if not authenticated
        if (!isAuthenticated) {
            login(window.location.href);
            return null;
        }

        setLoading(true);
        setError(null);

        try {
            // Protected endpoint - Gateway handles auth via session cookie
            const response = await createProduct({ body: data });

            if (response.error) {
                const apiError = parseApiError(response.error);

                // Handle auth errors specifically
                if (apiError.status === 401) {
                    login(window.location.href);
                    return null;
                }

                setError(apiError);
                return null;
            }

            return response.data ?? null;
        } catch (err) {
            setError(parseApiError(err));
            return null;
        } finally {
            setLoading(false);
        }
    }, [isAuthenticated, login]);

    return { create, loading, error };
}
```

### Step 3: Create Components

**Public component (no auth required):**

```tsx
// frontend/src/features/products/components/ProductList.tsx

import { useProducts } from "../hooks";

export function ProductList() {
    const { products, loading, error, meta, refresh } = useProducts();

    if (loading) return <p>Loading products...</p>;
    if (error) return <p>Error: {error.detail}</p>;

    return (
        <div>
            <h2>Products ({meta?.totalElements ?? 0})</h2>
            <ul>
                {products.map((product) => (
                    <li key={product.id}>
                        {product.name} - ${product.price}
                    </li>
                ))}
            </ul>
            <button onClick={refresh}>Refresh</button>
        </div>
    );
}
```

**Protected component (requires auth):**

```tsx
// frontend/src/features/products/components/CreateProductForm.tsx

import { useState } from "react";
import { useProductMutations } from "../hooks";
import { useAuth } from "../../auth/hooks";

export function CreateProductForm({ onSuccess }: { onSuccess?: () => void }) {
    const { isAuthenticated } = useAuth();
    const { create, loading, error } = useProductMutations();
    const [name, setName] = useState("");
    const [price, setPrice] = useState("");

    // Show login prompt if not authenticated
    if (!isAuthenticated) {
        return (
            <div>
                <p>Please log in to create products.</p>
                <LoginButton returnUrl={window.location.href}>
                    Log in
                </LoginButton>
            </div>
        );
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        const result = await create({
            name,
            price: parseFloat(price),
        });

        if (result) {
            setName("");
            setPrice("");
            onSuccess?.();
        }
    };

    return (
        <form onSubmit={handleSubmit}>
            <h3>Create Product</h3>

            {error && <p style={{ color: "red" }}>{error.detail}</p>}

            <div>
                <label>Name:</label>
                <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                />
            </div>

            <div>
                <label>Price:</label>
                <input
                    type="number"
                    step="0.01"
                    value={price}
                    onChange={(e) => setPrice(e.target.value)}
                    required
                />
            </div>

            <button type="submit" disabled={loading}>
                {loading ? "Creating..." : "Create Product"}
            </button>
        </form>
    );
}
```

### Step 4: Export from Feature Module

```typescript
// frontend/src/features/products/hooks/index.ts
export { useProducts } from "./useProducts";
export { useProductMutations } from "./useProductMutations";

// frontend/src/features/products/index.ts
export * from "./hooks";
export * from "./components/ProductList";
export * from "./components/CreateProductForm";
```

---

## Quick Reference

### OpenAPI Security Markers

| Security Setting                         | Meaning                | HTTP Response on Failure |
| ---------------------------------------- | ---------------------- | ------------------------ |
| `security: []`                           | Public endpoint        | N/A                      |
| `security: - BearerAuth: []`             | Requires valid JWT     | 401 Unauthorized         |
| With `@PreAuthorize("hasRole('ADMIN')")` | Requires specific role | 403 Forbidden            |

### Checklist: New Endpoint

- [ ] **OpenAPI Spec** (`api/specification/openapi.yaml`)
  - [ ] Add path with operation
  - [ ] Set `security: []` (public) or `security: - BearerAuth: []` (protected)
  - [ ] Define request/response schemas
  - [ ] Include `401`/`403` responses for protected endpoints

- [ ] **Backend** (`backend/`)
  - [ ] Run `./mvnw generate-sources` to regenerate API interfaces
  - [ ] Create/update controller implementing the generated interface
  - [ ] Update `permit-all` in properties if endpoint is public
  - [ ] Add `@PreAuthorize` annotations for role-based access

- [ ] **Frontend** (`frontend/`)
  - [ ] Run `npm run generate:api` to regenerate client
  - [ ] Create custom hook for the endpoint
  - [ ] Handle 401/403 responses in protected endpoints
  - [ ] Create component using the hook

### Common Patterns

```yaml
# OpenAPI: Public endpoint
security: []

# OpenAPI: Protected endpoint
security:
  - BearerAuth: []
```

```java
// Java: Access authenticated user
public ResponseEntity<?> myEndpoint(Authentication auth) {
    String username = auth.getName();
}

// Java: Role-based access
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOnly() { }
```

```typescript
// TypeScript: Check auth before mutation
const { isAuthenticated, login } = useAuth();
if (!isAuthenticated) {
    login(window.location.href);
    return;
}
```

---

## See Also

- [Development Cheatsheet](./dev-cheatsheet.md) — Commands for building and testing
- [API Specification](/openapi.yaml) — Live OpenAPI documentation
- [Architecture Overview](/docs/architecture/intro) — System design details

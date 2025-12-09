# Frontend Application

React + TypeScript + Vite application with OpenAPI-driven development.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Development Modes](#development-modes)
- [Testing](#testing)
- [API Client](#api-client)
- [Project Structure](#project-structure)

## Prerequisites

- Node.js 20+
- npm 10+

## Getting Started

Install dependencies:

```bash
npm install
```

Generate API client from OpenAPI specification:

```bash
npm run api:generate
```

## Development Modes

### Normal Development (with Backend)

Develop with a running backend server:

```bash
npm run dev
```

**Requirements**: Backend must be running on `http://localhost:8080`

**How it works**:
- Vite proxies `/api/*` requests to the backend at port 8080
- Full CRUD operations with real database

**Use when**:
- Developing features that require backend logic
- Testing full-stack integration
- Working with real data

---

### Mock Development (without Backend)

Develop frontend independently using Prism mock server:

```bash
npm run dev:mock
```

**Requirements**: None! No backend needed.

**How it works**:
- Starts Prism mock server on port 8080 (serves OpenAPI examples)
- Starts Vite dev server with hot reload
- Vite proxy rewrites `/api` requests to match Prism paths
- Returns realistic data from OpenAPI specification

**Use when**:
- Backend is unavailable or not started
- Developing UI components independently
- Rapid prototyping without backend setup
- Backend developers testing frontend requirements

**Example response** (from OpenAPI examples):
```json
{
  "data": [
    {
      "id": 1001,
      "reference": "GRE-2025-000001",
      "message": "Hello, World!",
      "recipient": "World",
      "createdAt": "2025-01-15T10:30:00Z"
    }
  ],
  "meta": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 3,
    "totalPages": 1
  }
}
```

---

### Standalone Mock Server

Run only the mock API server (useful for backend developers):

```bash
npm run mock-api
```

**Access**: http://localhost:8080/v1/greetings

**Use when**:
- Backend developers need to test API contract compliance
- Testing API client libraries
- Generating sample requests/responses

---

## Testing

### Unit & Integration Tests

```bash
npm run test              # Run tests in watch mode
npm run test:coverage     # Run tests with coverage report
```

**Test Stack**:
- Vitest (test runner)
- React Testing Library
- MSW (Mock Service Worker) for API mocking

**How API mocking works in tests**:
- MSW intercepts network requests at the application level
- Uses same request handlers for all tests
- Stateful in-memory store for CRUD testing
- No need to run mock server during tests

---

### End-to-End Tests

#### Against Real Backend

```bash
npm run test:e2e
```

**Requirements**: Backend running on port 8080

**Use when**: Testing full integration with real database

#### Against Mock Server (Faster)

```bash
npm run test:e2e:mock
```

**Requirements**: None! Prism starts automatically

**Benefits**:
- Faster execution (~30s vs ~60s)
- No database setup/teardown
- Consistent test data
- No backend startup time

**Use when**:
- Quick UI validation
- CI pipelines (no infrastructure needed)
- Testing against consistent data

---

## API Client

### Code Generation

API client is auto-generated from OpenAPI specification:

```bash
npm run api:generate
```

**Source**: `../api/specification/openapi.yaml`
**Output**: `src/api/generated/`

**Generated files**:
- `sdk.gen.ts` - API functions (listGreetings, createGreeting, etc.)
- `types.gen.ts` - TypeScript interfaces
- `client.gen.ts` - HTTP client configuration

### Usage Example

```typescript
import { listGreetings, createGreeting } from './api/generated';

// List greetings with pagination
const response = await listGreetings({
  query: { page: 0, size: 20 }
});

// Create a new greeting
const newGreeting = await createGreeting({
  body: {
    message: "Hello, World!",
    recipient: "World"
  }
});
```

### Type Safety

All requests and responses are fully typed:

```typescript
import type {
  GreetingResponse,
  CreateGreetingRequest,
  GreetingPage
} from './api/generated';
```

---

## Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── generated/           # Auto-generated API client (don't edit!)
│   │   ├── config.ts            # API client configuration
│   │   └── errors.ts            # Error handling utilities
│   ├── features/
│   │   └── greetings/           # Greeting feature module
│   │       ├── components/      # React components
│   │       ├── hooks/           # Custom React hooks
│   │       └── types/           # Feature-specific types
│   ├── test/
│   │   └── mocks/
│   │       ├── handlers.ts      # MSW request handlers
│   │       ├── data.ts          # Mock data factories
│   │       └── server.ts        # MSW server setup
│   ├── App.tsx                  # Root component
│   └── main.tsx                 # Application entry point
├── e2e/                         # Playwright E2E tests
├── playwright.config.ts         # Playwright configuration (real backend)
├── playwright.config.mock.ts    # Playwright configuration (Prism)
├── openapi-ts.config.ts         # API client generator config
├── vite.config.ts               # Vite bundler config
└── package.json                 # Dependencies & scripts
```

---

## Scripts Reference

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server (requires backend) |
| `npm run dev:mock` | Start development with mock server (no backend) |
| `npm run mock-api` | Start Prism mock server only |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build |
| `npm run test` | Run unit tests in watch mode |
| `npm run test:coverage` | Run tests with coverage |
| `npm run test:e2e` | Run E2E tests against real backend |
| `npm run test:e2e:mock` | Run E2E tests against Prism |
| `npm run api:generate` | Generate API client from OpenAPI spec |
| `npm run lint` | Lint code with ESLint |
| `npm run typecheck` | Type-check with TypeScript |
| `npm run format` | Format code with Prettier |

---

## FAQ

### Q: Why do I get a 404 error when calling the API?

**A**: Check which mode you're using:
- **Real backend**: Ensure backend is running on port 8080
- **Mock mode**: Use `npm run dev:mock` instead of `npm run dev`

### Q: Mock server returns different data than expected?

**A**: Mock data comes from OpenAPI examples. Update the specification:
- File: `../api/specification/openapi.yaml`
- Add/modify `examples` in response definitions
- Restart mock server

### Q: Can I add custom mock scenarios for tests?

**A**: Yes! Edit test mock handlers:
- File: `src/test/mocks/handlers.ts`
- Add custom logic for specific test scenarios
- Use `mockErrors` utilities for error responses

### Q: How do I switch between backend and mock mode?

**A**:
- Backend: `npm run dev` (default)
- Mock: `npm run dev:mock` (sets VITE_USE_PRISM=true)
- No restart needed - just kill one and start the other

### Q: What's the difference between MSW (tests) and Prism (dev)?

**A**:
- **MSW**: In-process mocking for unit/integration tests (Node.js)
- **Prism**: Standalone HTTP server for development (separate process)
- **Both**: Serve OpenAPI-compliant responses

---

## Troubleshooting

### Port 8080 already in use

```bash
# Find process using port 8080
netstat -ano | findstr :8080    # Windows
lsof -i :8080                   # macOS/Linux

# Kill the process or use a different port
npm run mock-api -- --port 4010
```

### API client not generated

```bash
# Ensure OpenAPI spec exists
ls ../api/specification/openapi.yaml

# Regenerate client
npm run api:generate

# Check for errors in openapi-ts.config.ts
```

### Vite proxy not working

1. Check Vite dev server is running (default port: 5173)
2. Verify proxy configuration in `vite.config.ts`
3. Ensure backend/Prism is running on port 8080
4. Check browser DevTools Network tab for proxy errors

---

## Additional Resources

- [Vite Documentation](https://vite.dev/)
- [React Documentation](https://react.dev/)
- [Prism Documentation](https://stoplight.io/open-source/prism)
- [MSW Documentation](https://mswjs.io/)
- [Playwright Documentation](https://playwright.dev/)
- [hey-api/openapi-ts](https://github.com/hey-api/openapi-ts)

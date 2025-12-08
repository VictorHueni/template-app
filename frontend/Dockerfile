# =============================================================================
# Frontend Dockerfile - Multi-stage build
# =============================================================================
# Stage 1: Build the React/Vite application with Node.js
# Stage 2: Serve the built static files with nginx
# =============================================================================

# === STAGE 1: BUILD ===
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files first for better layer caching
COPY frontend/package*.json ./

# Copy the API specification (needed for openapi-ts code generation)
COPY api ../api

# Install dependencies
RUN npm ci

# Copy the rest of the frontend source code
COPY frontend/ ./

# Build the application (includes API generation)
RUN npm run build

# === STAGE 2: SERVE WITH NGINX ===
FROM nginx:alpine AS final

# Remove default nginx static content
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom nginx configuration
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Health check for the nginx server
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# Start nginx in foreground
CMD ["nginx", "-g", "daemon off;"]

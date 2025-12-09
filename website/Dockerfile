# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files
COPY website/package*.json ./

# Install dependencies
RUN npm ci

# Copy website source
COPY website/ ./

# Copy OpenAPI spec from api folder (context is root)
COPY api/specification/openapi.yaml static/openapi.yaml

# Build the site
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy built files
COPY --from=builder /app/build /usr/share/nginx/html

# Copy nginx config
COPY website/nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 3001

# Run nginx
CMD ["nginx", "-g", "daemon off;"]

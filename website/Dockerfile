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

# Build the site (run docusaurus directly to skip prebuild hook - OpenAPI already copied above)
RUN npm exec docusaurus build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Note: nginx:alpine already has nginx user/group, no need to create them

# Copy built files and change ownership
COPY --from=builder /app/build /usr/share/nginx/html
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d
RUN touch /var/run/nginx.pid && chown -R nginx:nginx /var/run/nginx.pid

# Switch to the non-root user
USER nginx

# Copy nginx config
COPY website/nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 3001

# Run nginx
CMD ["nginx", "-g", "daemon off;"]

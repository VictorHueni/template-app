---
sidebar_position: 2
---

# Rate Limiting Strategy

This document describes the rate limiting strategy for the Template Application. Rate limiting is handled at the infrastructure level, not within the application itself.

## Overview

Rate limiting protects the application from:
- **Brute force attacks** on authentication endpoints
- **Denial of Service (DoS)** attacks
- **Token enumeration** attempts
- **Resource exhaustion** from excessive API calls

## Decision: Infrastructure-Level Rate Limiting

Rate limiting is implemented at the infrastructure layer (nginx, WAF, or cloud provider) rather than in the application code for the following reasons:

1. **Separation of concerns** - Security infrastructure should handle network-level protections
2. **Performance** - Rejecting requests early in the stack reduces load on application servers
3. **Flexibility** - Infrastructure rules can be updated without application deployments
4. **Consistency** - Centralized rate limiting applies uniformly across all services

## Recommended Settings

### Authentication Endpoints

| Endpoint Pattern | Rate Limit | Per | Notes |
|------------------|------------|-----|-------|
| `/login/**` | 10 requests | minute per IP | Prevents brute force attacks |
| `/oauth2/**` | 10 requests | minute per IP | OAuth2 flow protection |
| `/logout/**` | 5 requests | minute per IP | Logout endpoint protection |

### API Endpoints

| Endpoint Pattern | Rate Limit | Per | Notes |
|------------------|------------|-----|-------|
| `/api/**` (authenticated) | 100 requests | minute per user | Normal API usage |
| `/api/**` (unauthenticated) | 20 requests | minute per IP | Public endpoint protection |

## Implementation Examples

### nginx Configuration

```nginx
# Define rate limiting zones
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=10r/m;
limit_req_zone $binary_remote_addr zone=api_public_limit:10m rate=20r/m;

# Apply to authentication endpoints
location ~ ^/(login|oauth2)/ {
    limit_req zone=auth_limit burst=5 nodelay;
    proxy_pass http://gateway:8080;
}

# Apply to logout endpoint
location /logout/ {
    limit_req zone=auth_limit burst=2 nodelay;
    proxy_pass http://gateway:8080;
}

# Apply to public API endpoints
location /api/ {
    limit_req zone=api_public_limit burst=10 nodelay;
    proxy_pass http://gateway:8080;
}
```

### AWS WAF Rules

```json
{
  "Name": "AuthRateLimit",
  "Priority": 1,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 100,
      "AggregateKeyType": "IP",
      "ScopeDownStatement": {
        "ByteMatchStatement": {
          "SearchString": "/login",
          "FieldToMatch": { "UriPath": {} },
          "TextTransformations": [{ "Priority": 0, "Type": "NONE" }],
          "PositionalConstraint": "STARTS_WITH"
        }
      }
    }
  },
  "Action": { "Block": {} }
}
```

### Cloudflare Rate Limiting

```yaml
# Cloudflare Rate Limiting Rule
- expression: '(http.request.uri.path matches "^/login" or http.request.uri.path matches "^/oauth2")'
  action: block
  ratelimit:
    characteristics:
      - ip.src
    period: 60
    requests_per_period: 10
    mitigation_timeout: 300
```

## Monitoring and Alerting

### Key Metrics to Monitor

1. **Rate limit hits per endpoint** - Track how often limits are triggered
2. **Unique IPs hitting limits** - Identify potential attackers
3. **Geographic distribution** - Detect regional attack patterns
4. **Time-based patterns** - Identify attack windows

### Recommended Alerts

| Metric | Threshold | Action |
|--------|-----------|--------|
| Rate limit hits (auth) | > 100/hour | Investigate potential brute force |
| Rate limit hits (API) | > 1000/hour | Check for API abuse |
| Unique blocked IPs | > 50/hour | Review for coordinated attack |

## Response Handling

When a request is rate limited:

1. **HTTP 429 Too Many Requests** should be returned
2. Include `Retry-After` header with seconds until the limit resets
3. Log the event for security analysis
4. Do not reveal specific rate limit thresholds in error messages

### Example Response

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Content-Type: application/json

{
  "error": "rate_limit_exceeded",
  "message": "Too many requests. Please try again later."
}
```

## Security Considerations

1. **Do not expose rate limit details** - Attackers can use this to optimize their attacks
2. **Use sliding windows** - Prevent burst attacks at window boundaries
3. **Consider user-based limits** - Authenticated users may need different limits than anonymous
4. **Implement progressive backoff** - Increase block duration for repeat offenders
5. **Log all rate limit events** - Essential for security incident investigation

## Related Documentation

- [ADR-003: Authentication and Identity](../architecture/09-architecture-decisions/ADR-003-authentication-and-identity.md)
- [Security Overview](../architecture/04-security.md)

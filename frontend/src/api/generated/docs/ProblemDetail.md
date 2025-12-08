
# ProblemDetail

RFC 7807 Problem Details for HTTP APIs. See: https://datatracker.ietf.org/doc/html/rfc7807 

## Properties

Name | Type
------------ | -------------
`type` | string
`title` | string
`status` | number
`detail` | string
`instance` | string
`timestamp` | Date
`traceId` | string
`errors` | { [key: string]: string; }
`resourceType` | string
`resourceId` | string
`reason` | string

## Example

```typescript
import type { ProblemDetail } from ''

// TODO: Update the object below with actual values
const example = {
  "type": https://api.example.com/problems/resource-not-found,
  "title": Resource Not Found,
  "status": 404,
  "detail": Greeting with id '123' not found,
  "instance": /api/v1/greetings/123,
  "timestamp": 2025-01-15T10:30:00Z,
  "traceId": 550e8400-e29b-41d4-a716-446655440000,
  "errors": {message=must not be blank, recipient=size must be between 1 and 100},
  "resourceType": Greeting,
  "resourceId": 123,
  "reason": Resource already exists,
} satisfies ProblemDetail

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProblemDetail
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



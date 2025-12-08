
# GreetingResponse


## Properties

Name | Type
------------ | -------------
`id` | number
`reference` | string
`message` | string
`recipient` | string
`createdAt` | Date

## Example

```typescript
import type { GreetingResponse } from ''

// TODO: Update the object below with actual values
const example = {
  "id": 506979954615549952,
  "reference": GRE-2025-000042,
  "message": null,
  "recipient": null,
  "createdAt": null,
} satisfies GreetingResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as GreetingResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)




# PatchGreetingRequest

Partial update - only provided fields are updated

## Properties

Name | Type
------------ | -------------
`message` | string
`recipient` | string

## Example

```typescript
import type { PatchGreetingRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "message": Hello, World!,
  "recipient": Universe,
} satisfies PatchGreetingRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PatchGreetingRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



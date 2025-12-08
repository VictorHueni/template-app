
# UpdateGreetingRequest

Full replacement update - all fields are required

## Properties

Name | Type
------------ | -------------
`message` | string
`recipient` | string

## Example

```typescript
import type { UpdateGreetingRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "message": Hello, World!,
  "recipient": Universe,
} satisfies UpdateGreetingRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateGreetingRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)



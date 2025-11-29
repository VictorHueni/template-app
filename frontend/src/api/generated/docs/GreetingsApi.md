# GreetingsApi

All URIs are relative to */api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createGreeting**](GreetingsApi.md#creategreetingoperation) | **POST** /greetings | Create a greeting |
| [**listGreetings**](GreetingsApi.md#listgreetings) | **GET** /greetings | List greetings |



## createGreeting

> GreetingResponse createGreeting(createGreetingRequest)

Create a greeting

### Example

```ts
import {
  Configuration,
  GreetingsApi,
} from '';
import type { CreateGreetingOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new GreetingsApi(config);

  const body = {
    // CreateGreetingRequest (optional)
    createGreetingRequest: ...,
  } satisfies CreateGreetingOperationRequest;

  try {
    const data = await api.createGreeting(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createGreetingRequest** | [CreateGreetingRequest](CreateGreetingRequest.md) |  | [Optional] |

### Return type

[**GreetingResponse**](GreetingResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Created |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listGreetings

> GreetingPage listGreetings(page, size)

List greetings

### Example

```ts
import {
  Configuration,
  GreetingsApi,
} from '';
import type { ListGreetingsRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new GreetingsApi();

  const body = {
    // number | The page number to retrieve (0-based index) (optional)
    page: 56,
    // number | The number of items per page (optional)
    size: 56,
  } satisfies ListGreetingsRequest;

  try {
    const data = await api.listGreetings(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **page** | `number` | The page number to retrieve (0-based index) | [Optional] [Defaults to `0`] |
| **size** | `number` | The number of items per page | [Optional] [Defaults to `20`] |

### Return type

[**GreetingPage**](GreetingPage.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A paged list of greetings |  -  |
| **400** | The server could not process the request due to invalid syntax. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


# GreetingsApi

All URIs are relative to *https://api.example.com/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createGreeting**](GreetingsApi.md#creategreetingoperation) | **POST** /v1/greetings | Create a greeting |
| [**deleteGreeting**](GreetingsApi.md#deletegreeting) | **DELETE** /v1/greetings/{id} | Delete a greeting |
| [**getGreeting**](GreetingsApi.md#getgreeting) | **GET** /v1/greetings/{id} | Get a greeting by ID |
| [**listGreetings**](GreetingsApi.md#listgreetings) | **GET** /v1/greetings | List greetings |
| [**patchGreeting**](GreetingsApi.md#patchgreetingoperation) | **PATCH** /v1/greetings/{id} | Partially update a greeting |
| [**updateGreeting**](GreetingsApi.md#updategreetingoperation) | **PUT** /v1/greetings/{id} | Update a greeting (full replacement) |



## createGreeting

> GreetingResponse createGreeting(createGreetingRequest)

Create a greeting

Creates a new greeting resource. Requires authentication. Returns the created greeting with generated ID and reference.

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
    // CreateGreetingRequest
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
| **createGreetingRequest** | [CreateGreetingRequest](CreateGreetingRequest.md) |  | |

### Return type

[**GreetingResponse**](GreetingResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Created |  -  |
| **400** | The server could not process the request due to invalid syntax. |  -  |
| **401** | Authentication is required to access this resource. |  -  |
| **403** | You do not have permission to access this resource. |  -  |
| **409** | The request conflicts with the current state of the resource. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteGreeting

> deleteGreeting(id)

Delete a greeting

Permanently removes a greeting resource. This action cannot be undone. Requires authentication.

### Example

```ts
import {
  Configuration,
  GreetingsApi,
} from '';
import type { DeleteGreetingRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new GreetingsApi(config);

  const body = {
    // number | The unique identifier (TSID) of the greeting
    id: 506979954615549952,
  } satisfies DeleteGreetingRequest;

  try {
    const data = await api.deleteGreeting(body);
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
| **id** | `number` | The unique identifier (TSID) of the greeting | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Greeting deleted successfully |  -  |
| **401** | Authentication is required to access this resource. |  -  |
| **403** | You do not have permission to access this resource. |  -  |
| **404** | The requested resource was not found. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getGreeting

> GreetingResponse getGreeting(id)

Get a greeting by ID

Retrieves a single greeting by its unique TSID identifier.

### Example

```ts
import {
  Configuration,
  GreetingsApi,
} from '';
import type { GetGreetingRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const api = new GreetingsApi();

  const body = {
    // number | The unique identifier (TSID) of the greeting
    id: 506979954615549952,
  } satisfies GetGreetingRequest;

  try {
    const data = await api.getGreeting(body);
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
| **id** | `number` | The unique identifier (TSID) of the greeting | [Defaults to `undefined`] |

### Return type

[**GreetingResponse**](GreetingResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The greeting |  -  |
| **404** | The requested resource was not found. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listGreetings

> GreetingPage listGreetings(page, size)

List greetings

Retrieves a paginated list of all greetings. Supports pagination via page and size query parameters.

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
    page: 0,
    // number | The number of items per page (optional)
    size: 20,
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
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | A paged list of greetings |  -  |
| **400** | The server could not process the request due to invalid syntax. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## patchGreeting

> GreetingResponse patchGreeting(id, patchGreetingRequest)

Partially update a greeting

Performs a partial update of an existing greeting. Only provided fields are updated. Requires authentication.

### Example

```ts
import {
  Configuration,
  GreetingsApi,
} from '';
import type { PatchGreetingOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new GreetingsApi(config);

  const body = {
    // number | The unique identifier (TSID) of the greeting
    id: 506979954615549952,
    // PatchGreetingRequest
    patchGreetingRequest: ...,
  } satisfies PatchGreetingOperationRequest;

  try {
    const data = await api.patchGreeting(body);
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
| **id** | `number` | The unique identifier (TSID) of the greeting | [Defaults to `undefined`] |
| **patchGreetingRequest** | [PatchGreetingRequest](PatchGreetingRequest.md) |  | |

### Return type

[**GreetingResponse**](GreetingResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated greeting |  -  |
| **400** | The server could not process the request due to invalid syntax. |  -  |
| **401** | Authentication is required to access this resource. |  -  |
| **403** | You do not have permission to access this resource. |  -  |
| **404** | The requested resource was not found. |  -  |
| **409** | The request conflicts with the current state of the resource. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateGreeting

> GreetingResponse updateGreeting(id, updateGreetingRequest)

Update a greeting (full replacement)

Performs a full replacement update of an existing greeting. All fields must be provided. Requires authentication.

### Example

```ts
import {
  Configuration,
  GreetingsApi,
} from '';
import type { UpdateGreetingOperationRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new GreetingsApi(config);

  const body = {
    // number | The unique identifier (TSID) of the greeting
    id: 506979954615549952,
    // UpdateGreetingRequest
    updateGreetingRequest: ...,
  } satisfies UpdateGreetingOperationRequest;

  try {
    const data = await api.updateGreeting(body);
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
| **id** | `number` | The unique identifier (TSID) of the greeting | [Defaults to `undefined`] |
| **updateGreetingRequest** | [UpdateGreetingRequest](UpdateGreetingRequest.md) |  | |

### Return type

[**GreetingResponse**](GreetingResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | The updated greeting |  -  |
| **400** | The server could not process the request due to invalid syntax. |  -  |
| **401** | Authentication is required to access this resource. |  -  |
| **403** | You do not have permission to access this resource. |  -  |
| **404** | The requested resource was not found. |  -  |
| **409** | The request conflicts with the current state of the resource. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


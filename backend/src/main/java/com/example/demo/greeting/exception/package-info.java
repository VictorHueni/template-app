/**
 * Greeting module exceptions - internal to the module.
 *
 * <p>This package contains greeting-specific exceptions that are thrown
 * by the greeting module and handled by {@link com.example.demo.greeting.exception.GreetingExceptionHandler}.</p>
 *
 * <p>These exceptions extend {@link com.example.demo.common.exception.DomainException}
 * from the common module, but are not exposed outside the greeting module -
 * they are purely internal implementation details.</p>
 *
 * <p>This package follows Spring Modulith's module autonomy principle:
 * each module controls its own exception handling and error responses.</p>
 *
 * @since 1.1.0
 */
package com.example.demo.greeting.exception;

/**
 * User service API - Spring Security integration.
 *
 * <p>This package exposes the UserDetailsService implementation for
 * Spring Security authentication. It's marked as a named interface
 * to allow Spring Security (and potentially other modules) to discover
 * the authentication beans.</p>
 *
 * <p>Components in this package:</p>
 * <ul>
 *   <li>{@link com.example.demo.user.service.UserDetailsServiceImpl} -
 *       Implements Spring Security's UserDetailsService interface</li>
 * </ul>
 */
@org.springframework.modulith.NamedInterface("service")
package com.example.demo.user.service;

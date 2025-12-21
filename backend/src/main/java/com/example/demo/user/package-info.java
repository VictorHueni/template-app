/**
 * User module - User management and authentication.
 *
 * <p>This module is CLOSED by default and exposes only its service layer
 * through a named interface. Spring Security auto-discovers the UserDetailsService
 * bean via component scanning.</p>
 *
 * <h2>Public API (Named Interfaces)</h2>
 * <ul>
 *   <li><strong>service</strong>: UserDetailsService implementation for Spring Security</li>
 * </ul>
 *
 * <h2>Internal Components</h2>
 * <ul>
 *   <li><strong>domain</strong>: UserDetailsImpl entity (internal)</li>
 *   <li><strong>repository</strong>: UserRepository (internal)</li>
 *   <li><strong>config</strong>: SystemUserLoader initialization (internal)</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule
package com.example.demo.user;

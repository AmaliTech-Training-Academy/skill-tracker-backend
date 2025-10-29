# Task-Service Security Setup

## Current Configuration

Task-service uses a **custom SecurityConfig** instead of the shared `MicroserviceSecurityConfig` from `common-security`.

---

## Security Configuration

### **File:** `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/api/test/**").permitAll()
                .anyRequest().permitAll()  // ⚠️ ALL requests permitted
            )
            .addFilterBefore(
                new HeaderAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}
```

---

## How It Works

### **1. Header Authentication Filter**
```
Incoming Request
    ↓
HeaderAuthenticationFilter (from common-security)
    ├─ Reads X-User-Id header
    ├─ Reads X-User-Roles header
    └─ Sets SecurityContext
    ↓
Spring Security Context populated
    ↓
@PreAuthorize checks (if used)
    ↓
Controller/Service method
```

### **2. Current Security Features:**

✅ **CSRF Disabled** - Stateless API (correct for microservice)  
✅ **Stateless Sessions** - No session storage (correct)  
✅ **Header Filter** - Uses `HeaderAuthenticationFilter` from common-security  
✅ **Method Security Enabled** - `@EnableMethodSecurity` active  
⚠️ **Permit All Requests** - No HTTP-level authorization  

---

## Authorization Approach

### **HTTP-Level:** 
- ✅ `/actuator/**` - Permitted
- ✅ `/api/test/**` - Permitted  
- ⚠️ All other requests - **Also permitted**

### **Method-Level:**
Currently **NOT ENFORCED** - `@PreAuthorize` annotations are commented out:

```java
@GetMapping("/{id}")
// @PreAuthorize("isAuthenticated()")  ⬅️ COMMENTED OUT
public ResponseEntity<ApiResponse<TaskDTO>> getTaskById(@PathVariable UUID id) {
    // Anyone can access this endpoint
}
```

---

## Security Status

### **Current State:**

| Feature | Status | Notes |
|---------|--------|-------|
| CSRF Protection | ✅ Disabled | Correct for stateless API |
| Session Management | ✅ Stateless | Correct for microservice |
| Header Authentication | ✅ Enabled | Reads X-User-Id, X-User-Roles |
| SecurityContext Population | ✅ Working | User info available in context |
| HTTP-Level Authorization | ⚠️ Disabled | All requests permitted |
| Method-Level Authorization | ⚠️ Not Used | @PreAuthorize commented out |

### **Security Implications:**

⚠️ **Currently:** Any request that reaches task-service is processed  
⚠️ **Relies on:** API Gateway to block unauthenticated requests  
⚠️ **Risk:** If microservice is exposed directly, no authentication required  

---

## Comparison with Common-Security

### **Task-Service (Custom):**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // Custom configuration
    // Permits all requests at HTTP level
    // @PreAuthorize not actively used
}
```

### **Common-Security (Shared):**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class MicroserviceSecurityConfig {
    // Shared configuration for all microservices
    // Same approach: permit all, use method security
    // Auto-applies to services that don't have custom config
}
```

### **Differences:**

| Aspect | Task-Service | Common-Security |
|--------|--------------|-----------------|
| Configuration File | Custom `SecurityConfig.java` | Auto-applied `MicroserviceSecurityConfig` |
| Filter Registration | Manual instantiation | Bean injection |
| Actuator Endpoints | Explicitly permitted | All permitted |
| Test Endpoints | `/api/test/**` permitted | N/A |
| Method Security | `@EnableMethodSecurity` | `@EnableMethodSecurity(prePostEnabled = true)` |

---

## Getting Current User

Even though `@PreAuthorize` is not used, the SecurityContext is still populated:

```java
@Service
public class TaskService {
    
    public void someMethod() {
        Authentication auth = SecurityContextHolder.getContext()
                                  .getAuthentication();
        
        if (auth != null) {
            String userId = (String) auth.getPrincipal();
            Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
            
            log.info("Current user: {}, roles: {}", userId, roles);
        }
    }
}
```

---

## Recommended Changes (If Needed)

### **Option 1: Use Method Security**

Uncomment and add `@PreAuthorize` annotations:

```java
@GetMapping("/{id}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<TaskDTO>> getTaskById(@PathVariable UUID id) {
    // Only authenticated users can access
}

@PostMapping("/admin/generate")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> generateTasks(@RequestBody GenerateTaskRequest request) {
    // Only admins can generate tasks
}
```

### **Option 2: Remove Custom Config**

Delete `SecurityConfig.java` and let `MicroserviceSecurityConfig` from `common-security` auto-apply:

```bash
# Delete this file
rm src/main/java/com/amalitech/task/service/config/SecurityConfig.java
```

Benefits:
- ✅ Consistent with other services (like feedback-service)
- ✅ Less code to maintain
- ✅ Automatic updates when common-security is updated

### **Option 3: Enforce HTTP-Level Security**

Keep custom config but require authentication:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**", "/api/test/**").permitAll()
    .anyRequest().authenticated()  // ⬅️ Require authentication
)
```

---

## Security Best Practices

### **Current Setup is SAFE if:**
1. ✅ Task-service is NOT publicly accessible
2. ✅ All traffic routes through API Gateway
3. ✅ API Gateway validates JWT and adds headers
4. ✅ Network policies prevent direct access

### **Potential Issues if:**
1. ⚠️ Microservice is accidentally exposed (e.g., misconfigured load balancer)
2. ⚠️ Internal service calls without headers
3. ⚠️ Development/testing bypasses gateway

---

## Summary

**Task-Service Security Setup:**

✅ **Headers Processed:** X-User-Id and X-User-Roles read from gateway  
✅ **SecurityContext Set:** User information available in context  
✅ **Stateless:** No sessions, correct for microservices  
⚠️ **No Enforcement:** All HTTP requests permitted  
⚠️ **Method Security Available but Unused:** `@PreAuthorize` commented out  

**Architecture:** Relies entirely on API Gateway for authentication/authorization enforcement at the network boundary.

**Recommendation:** 
- If you want defense-in-depth, add `@PreAuthorize` annotations
- If you trust network isolation, current setup is acceptable
- Consider using `MicroserviceSecurityConfig` for consistency

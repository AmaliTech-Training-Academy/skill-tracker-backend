# Security Architecture - Skill Tracker Backend

## Overview

The security architecture follows a **distributed authentication pattern** where:
1. **API Gateway** validates JWT tokens and enriches requests
2. **User Service** handles OAuth2 authentication and JWT generation  
3. **Microservices** trust headers from the gateway for authorization

---

## Authentication Flow

### 1. **User Login (OAuth2)**

```
Client → API Gateway → User Service
                          ↓
                      OAuth2 Provider (Google/GitHub)
                          ↓
                      Generate JWT
                          ↓
                      Set Cookie (accessToken)
                          ↓
                      Client
```

**Components:**
- **User Service** (`user-service`)
  - Handles OAuth2 login with Google/GitHub
  - Generates JWT tokens with user claims
  - Sets `accessToken` HTTP-only cookie
  - Configuration: `WebSecurityConfig.java`

**JWT Claims:**
```json
{
  "userId": "uuid",
  "roles": ["USER", "ADMIN"],
  "email": "user@example.com",
  "iat": 1234567890,
  "exp": 1234567890
}
```

**JWT Algorithm:** HS256 (HMAC with SHA-256)

---

### 2. **API Request Flow**

```
Client (with accessToken cookie)
    ↓
API Gateway
    ↓
JwtGlobalFilter
    ├─ Extract token from cookie
    ├─ Validate JWT signature
    ├─ Check expiration
    └─ Extract claims (userId, roles)
    ↓
Enrich Request Headers
    ├─ X-User-Id: {userId}
    └─ X-User-Roles: {role1,role2}
    ↓
Route to Microservice
    ↓
HeaderAuthenticationFilter
    ├─ Read X-User-Id
    ├─ Read X-User-Roles
    └─ Set SecurityContext
    ↓
@PreAuthorize Method Security
    ↓
Business Logic
```

---

## Component Details

### **API Gateway** (`api-gateway`)

**Purpose:** 
- Single entry point for all client requests
- JWT validation and token enrichment
- Route requests to microservices

**Key Files:**
- `SecurityConfig.java` - Spring Security configuration
- `JwtGlobalFilter.java` - JWT validation filter
- `JwtGatewayConfig.java` - JWT secret configuration

**Configuration:**
```yaml
app:
  security:
    whitelist:
      - /api/v1/auth/**
      - /oauth2/**
      - /health
      - /actuator/health
```

**JWT Validation:**
```java
@Bean
public ReactiveJwtDecoder jwtDecoder() {
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
    
    return NimbusReactiveJwtDecoder.withSecretKey(secretKeySpec)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
}
```

**Header Enrichment:**
```java
private ServerHttpRequest enrichRequest(ServerHttpRequest request, Jwt jwt) {
    String userId = jwt.getClaim("userId");
    List<String> rolesList = jwt.getClaimAsStringList("roles");
    String rolesHeader = String.join(",", rolesList);
    
    return request.mutate()
            .header("X-User-Id", userId)
            .header("X-User-Roles", rolesHeader)
            .build();
}
```

---

### **Common Security** (`common-security`)

**Purpose:**
- Shared security configuration for all microservices
- Header-based authentication
- Method-level security

**Key Classes:**

#### **MicroserviceSecurityConfig**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class MicroserviceSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()  // Authorization via method security
            )
            .addFilterBefore(
                headerAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class
            );
        
        return http.build();
    }
}
```

**Design Decisions:**
- ✅ **Stateless** - No HTTP sessions created
- ✅ **CSRF Disabled** - Stateless API with no browser sessions
- ✅ **Permit All Requests** - Authorization delegated to `@PreAuthorize`
- ✅ **Custom Filter** - Extracts authentication from headers

#### **HeaderAuthenticationFilter**
```java
public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-User-Roles");
        
        if (userId != null && !userId.isEmpty()) {
            List<SimpleGrantedAuthority> authorities = 
                Arrays.stream(rolesHeader.split(","))
                      .map(SimpleGrantedAuthority::new)
                      .collect(Collectors.toList());
            
            Authentication authentication = 
                new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
                );
            
            SecurityContextHolder.getContext()
                .setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

### **Microservices** (task-service, feedback-service, etc.)

**Security Setup:**
1. Import `common-security` dependency
2. Headers automatically processed by `HeaderAuthenticationFilter`
3. Use `@PreAuthorize` for method-level security

**Example Usage:**
```java
@Service
public class TaskService {
    
    @PreAuthorize("hasRole('ADMIN')")
    public Task deleteTask(UUID taskId) {
        // Only admins can delete tasks
    }
    
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<Task> getUserTasks() {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext()
                                  .getAuthentication();
        String userId = (String) auth.getPrincipal();
        
        return taskRepository.findByUserId(userId);
    }
}
```

---

## Security Guarantees

### ✅ **What is Secured:**

1. **JWT Validation**
   - Signature verification (HS256)
   - Expiration check
   - Claim validation
   - Only at API Gateway (single point)

2. **Token Storage**
   - HTTP-only cookie (prevents XSS)
   - Secure flag in production (HTTPS only)
   - SameSite=Strict (CSRF protection)

3. **Header Enrichment**
   - Automatic user context propagation
   - Cannot be spoofed (internal network)
   - Consistent across all services

4. **Method-Level Authorization**
   - `@PreAuthorize` annotations
   - Role-based access control
   - Fine-grained permissions

---

## Security Considerations

### 🔒 **Critical Requirements:**

1. **Network Isolation**
   - ⚠️ Microservices MUST NOT be directly accessible from internet
   - ⚠️ All traffic MUST route through API Gateway
   - ⚠️ Use network policies/security groups to enforce

2. **Header Trust Model**
   - Microservices trust `X-User-Id` and `X-User-Roles` headers
   - This is SAFE only if gateway is the only entry point
   - Direct access to microservices = security vulnerability

3. **JWT Secret Management**
   - Secret key shared between user-service and gateway
   - MUST be strong (256+ bits)
   - MUST be stored securely (environment variables)
   - Rotate regularly in production

4. **Cookie Security**
   ```java
   cookie.setHttpOnly(true);  // Prevents XSS
   cookie.setSecure(true);    // HTTPS only (production)
   cookie.setSameSite("Strict"); // CSRF protection
   ```

---

## Configuration

### **Environment Variables Required:**

#### **API Gateway:**
```bash
JWT_SECRET=your-secret-key-here-minimum-256-bits
APP_SECURITY_WHITELIST=/api/v1/auth/**,/oauth2/**,/health
```

#### **User Service:**
```bash
JWT_SECRET=your-secret-key-here-minimum-256-bits
OAUTH2_CLIENT_ID=your-google-client-id
OAUTH2_CLIENT_SECRET=your-google-client-secret
OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
```

#### **Microservices:**
```bash
# No JWT secret needed - they trust headers from gateway
```

---

## Authorization Patterns

### **Role-Based Access Control (RBAC):**

```java
// Require specific role
@PreAuthorize("hasRole('ADMIN')")
public void adminOnly() { }

// Require any of multiple roles
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public void authenticatedUsers() { }

// Complex expressions
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
public void adminOrSelf(String userId) { }
```

### **Getting Current User:**

```java
@Service
public class MyService {
    
    public void doSomething() {
        Authentication auth = SecurityContextHolder.getContext()
                                  .getAuthentication();
        
        String userId = (String) auth.getPrincipal();
        Collection<? extends GrantedAuthority> roles = auth.getAuthorities();
        
        // Use userId and roles
    }
}
```

---

## Testing Security

### **Testing with JWT Token:**

1. **Login via OAuth2:**
   ```bash
   # Access gateway OAuth2 endpoint
   curl http://localhost:8080/oauth2/authorization/google
   ```

2. **Extract Cookie:**
   ```bash
   # Browser dev tools → Application → Cookies → accessToken
   ```

3. **Make Authenticated Request:**
   ```bash
   curl -X GET http://localhost:8080/api/v1/tasks \
     --cookie "accessToken=eyJhbGciOiJIUzI1..."
   ```

### **Testing Microservice Directly (DEV ONLY):**

```bash
# Add headers manually (simulating gateway)
curl -X GET http://localhost:8082/api/v1/tasks \
  -H "X-User-Id: 123e4567-e89b-12d3-a456-426614174000" \
  -H "X-User-Roles: USER,ADMIN"
```

⚠️ **This should NOT work in production** (network isolation)

---

## Security Checklist

### **Production Deployment:**

- [ ] Microservices NOT publicly accessible
- [ ] All traffic routes through API Gateway
- [ ] JWT secret is strong (256+ bits)
- [ ] JWT secret stored securely (env vars, secrets manager)
- [ ] Cookies use `Secure` flag (HTTPS)
- [ ] Cookies use `HttpOnly` flag
- [ ] CORS properly configured
- [ ] Rate limiting enabled on gateway
- [ ] Security headers configured
- [ ] OAuth2 redirect URIs whitelisted
- [ ] Regular security audits
- [ ] JWT secret rotation policy

---

## Architecture Benefits

✅ **Single Point of Authentication** - API Gateway only  
✅ **Simplified Microservices** - No JWT validation needed  
✅ **Consistent Security Context** - Same headers everywhere  
✅ **Method-Level Security** - Fine-grained control  
✅ **Stateless** - Scales horizontally  
✅ **Token in Cookie** - XSS protection  
✅ **OAuth2 Integration** - Social login support  

---

## Summary

The security architecture uses:
1. **API Gateway** - JWT validation + header enrichment
2. **User Service** - OAuth2 login + JWT generation
3. **Common Security** - Header-based authentication for microservices
4. **Method Security** - `@PreAuthorize` for authorization

This provides **robust, scalable, and maintainable security** for a microservices architecture.

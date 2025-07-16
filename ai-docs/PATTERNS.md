# Design Patterns and Code Conventions

## Overview

This document outlines the design patterns, coding conventions, and best practices used in the Keycloak Facade Claim Mapper project.

## Design Patterns

### Provider Pattern
The mapper implements Keycloak's provider pattern through the `ProtocolMapper` interface:
- Service discovery via `META-INF/services`
- Configuration through provider properties
- Lifecycle managed by Keycloak

### Single Responsibility Principle
Each method has a clear, focused purpose:
- `isRequestFromFacade()` - Header detection logic
- `isValidFacadeUrl()` - URL validation logic
- `transformAccessToken()` - Token transformation orchestration

### Configuration as Code
- Static configuration properties defined at class loading
- Type-safe property definitions
- Default values prevent null pointer exceptions

## Code Organization

### Package Structure
```
com.agronod.keycloak/
├── FacadeOIDCProtocolMapper.java    # Main mapper implementation
└── (test packages mirror main structure)
```

### Class Structure
1. Constants (public static final)
2. Static configuration block
3. Configuration getter methods
4. Provider metadata methods
5. Core business logic methods
6. Private helper methods

## Coding Conventions

### Naming Conventions
- **Classes**: PascalCase (e.g., `FacadeOIDCProtocolMapper`)
- **Methods**: camelCase with descriptive verbs (e.g., `isRequestFromFacade`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `FACADE_HEADER_NAME`)
- **Variables**: camelCase with meaningful names

### Method Documentation
Every public method includes:
- Purpose description
- Parameter documentation
- Return value documentation
- Algorithm steps (for complex methods)

Example:
```java
/**
 * Checks if the current request came through the authentication facade.
 * 
 * @param keycloakSession The current Keycloak session
 * @return true if the facade header is present with the expected value
 */
```

### Constants Management
- All magic strings extracted to constants
- Grouped by purpose
- Clear, descriptive names

```java
public static final String PROVIDER_ID = "oidc-facade-issuer-mapper";
private static final String FACADE_HEADER_NAME = "X-Auth-Facade-Request";
private static final String FACADE_HEADER_VALUE = "true";
```

## Error Handling

### Graceful Degradation
- Invalid configurations don't break token generation
- Warnings logged for configuration issues
- Original token returned on any error

### Validation Pattern
```java
if (isValidFacadeUrl(url)) {
    // Happy path
} else if (url != null && !url.trim().isEmpty()) {
    // Log warning for invalid configuration
    logger.warn("Invalid facade issuer URL configured: " + url);
}
```

### Null Safety
- Explicit null checks before operations
- Use of trim() to handle whitespace
- Safe string comparisons (constant.equals(variable))

## Logging Best Practices

### Log Levels
- **DEBUG**: Successful operations (issuer override)
- **WARN**: Configuration problems (invalid URLs)
- **ERROR**: Not used (failures handled gracefully)

### Log Content
- Include relevant context (URLs, configurations)
- Avoid logging sensitive data
- Clear, actionable messages

## Testing Patterns

### Unit Test Organization
- Given-When-Then structure
- Descriptive test method names
- One assertion focus per test

Example:
```java
@Test
void transformAccessToken_WithFacadeHeaderAndValidUrl_OverridesIssuer() {
    // Given
    String expectedIssuer = "https://facade.example.com";
    
    // When
    AccessToken result = mapper.transformAccessToken(...);
    
    // Then
    assertEquals(expectedIssuer, result.getIssuer());
}
```

### Test Coverage Focus
- Happy path scenarios
- Edge cases (empty/null/invalid inputs)
- Configuration variations
- Provider metadata

## Performance Considerations

### Efficiency Principles
- No unnecessary object creation
- Direct method calls (no reflection)
- Early returns for quick decisions
- Minimal string operations

### Resource Management
- No resources to manage (no DB, no files)
- Stateless operations
- No caching needed

## Security Patterns

### Input Validation
- URL format validation before use
- Header value exact matching
- No user input in log messages

### Least Privilege
- Only modifies issuer claim
- No access to sensitive token data
- Read-only session access

## Configuration Patterns

### Property Definition
```java
ProviderConfigProperty property = new ProviderConfigProperty();
property.setName("facadeIssuerUrl");
property.setLabel("Facade Issuer URL");
property.setHelpText("Descriptive help text");
property.setType(ProviderConfigProperty.STRING_TYPE);
property.setDefaultValue("");
```

### Configuration Access
```java
String configValue = mappingModel.getConfig().get("propertyName");
```

## Keycloak Integration Patterns

### Interface Implementation
- Implement all required token mapper interfaces
- Extend `AbstractOIDCProtocolMapper` for common functionality
- Override only necessary methods

### Service Registration
```
META-INF/services/org.keycloak.protocol.ProtocolMapper
└── com.agronod.keycloak.FacadeOIDCProtocolMapper
```

## Import Organization
Group imports by type:
```java
// Standard Java imports first
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// Third-party libraries
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
```

## Code Quality Standards

### Method Length
- Keep methods under 20 lines
- Extract complex logic to helper methods
- Single responsibility per method

### Cyclomatic Complexity
- Avoid deep nesting
- Use early returns
- Extract complex conditions to named methods

### Comments
- Use Javadoc for all public methods with comprehensive documentation
- Include inline comments when they explain "why" or provide important context
- Document algorithm steps in method Javadoc for complex logic
- Avoid redundant comments that merely restate what the code does

Example of good inline comment usage:
```java
if (isValidFacadeUrl(facadeIssuerUrl)) {
    String trimmedUrl = facadeIssuerUrl.trim();
    token.issuer(trimmedUrl);
    logger.debug("Overrode token issuer to facade URL: " + trimmedUrl);
} else if (facadeIssuerUrl != null && !facadeIssuerUrl.trim().isEmpty()) {
    // URL is configured but invalid
    logger.warn("Invalid facade issuer URL configured: " + facadeIssuerUrl);
}
```

Example of algorithm documentation:
```java
/**
 * Transforms the access token by potentially overriding the issuer URL.
 * 
 * Algorithm:
 * 1. Check if the request contains the facade header
 * 2. If header is present, retrieve the configured facade issuer URL
 * 3. Validate the URL format
 * 4. Override the token issuer if valid
 * 
 * @param token The access token to transform
 * @param mappingModel The protocol mapper configuration
 * @param keycloakSession The current Keycloak session
 * @param userSession The user session
 * @param clientSessionCtx The client session context
 * @return The transformed access token
 */
```

## Anti-Patterns to Avoid

❌ **Don't hardcode configuration values** - Use configuration properties
❌ **Don't catch exceptions without logging** - Always log errors with context
❌ **Don't ignore null checks** - Validate inputs before use
❌ **Don't mix concerns** - Keep validation, transformation, and configuration separate
❌ **Don't log sensitive data** - Be careful with token contents in logs
# Architecture Overview

## System Purpose

This project provides a lightweight Keycloak protocol mapper that overrides the token issuer URL when requests are proxied through an authentication facade. It enables tokens to appear as if they were issued by the facade rather than the underlying Keycloak instance.

## Component Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                        Keycloak Server                          │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              FacadeOIDCProtocolMapper                       │ │
│  │  - Implements OIDCAccessTokenMapper                        │ │
│  │  - Implements OIDCIDTokenMapper                           │ │
│  │  - Implements UserInfoTokenMapper                         │ │
│  │  - Detects facade requests via HTTP headers               │ │
│  │  - Overrides token issuer URL when appropriate            │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│                                ▼                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │           Request Context (KeycloakSession)                │ │
│  │  - Provides access to HTTP headers                        │ │
│  │  - Contains request-specific context                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Key Classes

#### FacadeOIDCProtocolMapper
- **Purpose**: Main protocol mapper implementation
- **Responsibilities**:
  - Check for facade header presence
  - Validate configured facade issuer URL
  - Override token issuer when conditions are met
  - Provide configuration UI metadata

## Data Flow

### Token Enhancement Process

1. **Trigger**: Keycloak initiates token creation for authenticated user
2. **Header Detection**: Check for `X-Auth-Facade-Request: true` header
3. **Configuration Retrieval**: Get configured facade issuer URL
4. **URL Validation**: Ensure the configured URL is well-formed
5. **Issuer Override**: If header present and URL valid, replace token issuer
6. **Return**: Token with potentially overridden issuer

### Decision Flow

```
Token Creation Request
        │
        ▼
┌──────────────────┐
│ Check Header     │
│ X-Auth-Facade-   │──No──► Return Original Token
│ Request: true?   │
└──────────────────┘
        │ Yes
        ▼
┌──────────────────┐
│ Get Config URL   │
│ facadeIssuerUrl  │──Empty──► Return Original Token
└──────────────────┘
        │ Present
        ▼
┌──────────────────┐
│ Validate URL     │
│ Format           │──Invalid──► Log Warning & Return Original
└──────────────────┘
        │ Valid
        ▼
┌──────────────────┐
│ Override Issuer  │
│ in Token         │
└──────────────────┘
        │
        ▼
Return Modified Token
```

## Configuration

### Mapper Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| facadeIssuerUrl | String | "" | URL to use as issuer when facade header is detected |

### Example Configuration
```
facadeIssuerUrl: https://auth-facade.example.com
```

## Security Considerations

### Header-Based Detection
- Relies on `X-Auth-Facade-Request` header
- Header should only be set by trusted facade/proxy
- No authentication of the header itself

### URL Validation
- Validates URL format before use
- Trims whitespace to prevent configuration errors
- Logs warnings for invalid URLs

### Token Integrity
- Only modifies the issuer claim
- All other token claims remain unchanged
- Original signing and encryption preserved

## Performance Characteristics

### Efficiency
- Minimal overhead - simple header check and string replacement
- No external dependencies or network calls
- No database connections or complex computations

### Scalability
- Stateless operation
- No shared state between requests
- Scales linearly with Keycloak instances

## Deployment Architecture

### Keycloak Integration
- Deployed as JAR in `/opt/keycloak/providers/`
- Service discovery via `META-INF/services/org.keycloak.protocol.ProtocolMapper`
- Configuration through Keycloak admin console

### Dependencies
- Keycloak 26.0.0+ required
- No external runtime dependencies
- All dependencies marked as provided (uses Keycloak's libraries)

### Build Artifacts
- Main JAR: `facade_claim_mapper-1.0.0.jar`
- Fat JAR: `keycloak-facade-claim-mapper-jar-with-dependencies.jar`
- Maven assembly plugin creates deployable artifact

## Testing Strategy

### Unit Testing
- Provider metadata validation
- Configuration property testing
- Interface implementation verification
- Note: Full token transformation testing requires Keycloak runtime

### Integration Testing
- Deploy to test Keycloak instance
- Configure mapper with test facade URL
- Send requests with/without facade header
- Verify token issuer matches expectations

### Local Development
- Docker-based Keycloak with mounted JAR
- Test with curl or authentication proxy
- Verify token contents with jwt.io or similar

## Extension Points

### Adding New Headers
To check additional headers:
1. Add new constants for header name/value
2. Update `isRequestFromFacade` method
3. Document the new header requirement

### Multiple Facade URLs
To support multiple facades:
1. Change configuration to support multiple URLs
2. Add logic to select URL based on header value
3. Update validation to handle multiple URLs

### Additional Token Modifications
To modify other token fields:
1. Add new configuration properties
2. Update `transformAccessToken` method
3. Ensure modifications are appropriate for all token types
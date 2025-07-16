# Development Workflows

## Local Development

### Prerequisites
- Java 11 or higher
- Maven
- Docker (for testing)
- Keycloak 26.0.0+

### Building the Project

```bash
# Clean and build
mvn clean package

# This creates:
# - target/facade_claim_mapper-1.0.0.jar (main JAR)
# - target/keycloak-facade-claim-mapper-jar-with-dependencies.jar (fat JAR)
```

### Running Tests

```bash
# Run all tests
mvn test

# Run with specific test
mvn test -Dtest=FacadeOIDCProtocolMapperTest
```

## Local Testing with Docker

### Quick Start

```bash
# Build and run Keycloak with the mapper
mvn clean package && docker run --name keycloak \
  -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -v "$(pwd)/target/keycloak-facade-claim-mapper-jar-with-dependencies.jar:/opt/keycloak/providers/keycloak-facade-claim-mapper-jar-with-dependencies.jar" \
  quay.io/keycloak/keycloak:26.0.0 \
  start-dev
```

### Configuration in Keycloak

1. **Access Admin Console**
   - Navigate to http://localhost:8080/admin
   - Login with admin/admin

2. **Create Client Scope**
   - Go to Client Scopes → Create
   - Name: `facade-mapper`
   - Protocol: `openid-connect`

3. **Add Mapper**
   - In the client scope, go to Mappers tab
   - Click "Configure a new mapper"
   - Select "Facade Issuer Mapper"
   - Configure:
     - Name: `facade-issuer-override`
     - Facade Issuer URL: `https://auth-facade.example.com`

4. **Assign to Client**
   - Go to Clients → Select your client
   - Client Scopes tab → Add client scope
   - Add `facade-mapper` as default or optional

## Testing the Mapper

### Without Facade Header

```bash
# Get access token normally
curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=your-client" \
  -d "client_secret=your-secret" \
  -d "grant_type=client_credentials"

# Decode token and check issuer (should be Keycloak's URL)
```

### With Facade Header

To test with the facade header, you need an authentication proxy that adds the header. For manual testing:

1. **Use a reverse proxy** (nginx, Apache, etc.) that adds the header
2. **Configure your application** to send authentication requests through the proxy
3. **Verify the token** has the facade issuer URL

Example nginx configuration:
```nginx
location /auth/ {
    proxy_pass http://localhost:8080/;
    proxy_set_header X-Auth-Facade-Request true;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

## Debugging

### Enable Debug Logging

```bash
# Add to Docker run command
-e KC_LOG_LEVEL=debug
```

### View Logs

```bash
# Docker logs
docker logs keycloak -f

# Look for mapper logs
docker logs keycloak | grep FacadeOIDCProtocolMapper
```

### Common Issues

#### Mapper Not Found
**Symptom**: Mapper doesn't appear in Keycloak UI
**Solution**: 
- Check JAR is in providers directory
- Restart Keycloak
- Check logs for loading errors

#### Invalid URL Configuration
**Symptom**: Warning in logs about invalid URL
**Solution**: 
- Check URL format in mapper configuration
- Ensure URL includes protocol (https://)
- Remove any trailing spaces

#### Header Not Detected
**Symptom**: Token issuer not overridden despite configuration
**Solution**:
- Verify header name is exactly `X-Auth-Facade-Request`
- Check header value is exactly `true`
- Ensure proxy/facade is adding the header

## Deployment

### Production Deployment

1. **Build the JAR**
   ```bash
   mvn clean package
   ```

2. **Copy to Keycloak**
   ```bash
   cp target/keycloak-facade-claim-mapper-jar-with-dependencies.jar \
      /opt/keycloak/providers/
   ```

3. **Set Permissions**
   ```bash
   chown keycloak:keycloak /opt/keycloak/providers/keycloak-facade-claim-mapper-jar-with-dependencies.jar
   chmod 644 /opt/keycloak/providers/keycloak-facade-claim-mapper-jar-with-dependencies.jar
   ```

4. **Restart Keycloak**
   ```bash
   systemctl restart keycloak
   ```

### Configuration Management

For production, consider:
- Using Keycloak's import/export for realm configuration
- Infrastructure as Code (Terraform, Ansible) for consistent deployments
- Separate facade URLs for different environments

## Extending the Mapper

### Adding Configuration Options

1. **Add Property Definition**
   ```java
   static {
       // Existing properties...
       
       ProviderConfigProperty property = new ProviderConfigProperty();
       property.setName("newProperty");
       property.setLabel("New Property");
       property.setHelpText("Description");
       property.setType(ProviderConfigProperty.STRING_TYPE);
       property.setDefaultValue("default");
       configProperties.add(property);
   }
   ```

2. **Use in Transformation**
   ```java
   String newValue = mappingModel.getConfig().get("newProperty");
   ```

### Supporting Multiple Headers

To check multiple headers:

1. **Add Constants**
   ```java
   private static final String SECONDARY_HEADER_NAME = "X-Other-Header";
   ```

2. **Update Detection Logic**
   ```java
   private boolean isRequestFromFacade(KeycloakSession keycloakSession) {
       HttpHeaders headers = keycloakSession.getContext().getRequestHeaders();
       return FACADE_HEADER_VALUE.equals(headers.getHeaderString(FACADE_HEADER_NAME)) ||
              FACADE_HEADER_VALUE.equals(headers.getHeaderString(SECONDARY_HEADER_NAME));
   }
   ```

### Modifying Other Token Claims

To modify additional claims:

1. **Add Logic to transformAccessToken**
   ```java
   if (isRequestFromFacade(keycloakSession)) {
       // Existing issuer override
       
       // Add custom claim
       token.getOtherClaims().put("facade_request", true);
       
       // Modify existing claim
       token.setPreferredUsername("facade-" + token.getPreferredUsername());
   }
   ```

## Maintenance

### Updating Dependencies

1. **Check for Updates**
   ```bash
   mvn versions:display-dependency-updates
   ```

2. **Update pom.xml**
   ```xml
   <keycloak.version>26.0.1</keycloak.version>
   ```

3. **Test Compatibility**
   ```bash
   mvn clean test
   ```

### Version Management

When releasing new versions:

1. **Update Version**
   ```xml
   <version>1.1.0</version>
   ```

2. **Tag Release**
   ```bash
   git tag -a v1.1.0 -m "Release version 1.1.0"
   git push origin v1.1.0
   ```

3. **Document Changes**
   - Update README.md
   - Add release notes
   - Update compatibility matrix

## Monitoring

### Performance Metrics

Monitor:
- Token generation time
- Mapper execution time
- Error rates

### Health Checks

Include in monitoring:
- Keycloak availability
- Mapper loading status
- Configuration validation

### Logging Best Practices

1. **Structured Logging**
   ```java
   logger.debugf("Facade request detected for session %s", sessionId);
   ```

2. **Error Context**
   ```java
   logger.warnf("Invalid URL configured: %s for mapper %s", url, mapperId);
   ```

3. **Performance Logging**
   ```java
   long start = System.currentTimeMillis();
   // ... operation ...
   logger.debugf("Token transformation took %d ms", System.currentTimeMillis() - start);
   ```
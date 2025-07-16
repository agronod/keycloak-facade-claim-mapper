# Keycloak Facade Claim Mapper

A lightweight Keycloak protocol mapper that overrides the token issuer URL when requests come through an authentication facade.

## Overview

This mapper detects when authentication requests are proxied through a facade by checking for the `X-Auth-Facade-Request` header. When detected, it replaces the token's issuer URL with a configured facade URL, ensuring tokens appear to be issued by the facade rather than the underlying Keycloak instance.

## Features

- Simple issuer URL override based on request headers
- No database dependencies
- Lightweight and focused on a single responsibility
- Compatible with Keycloak 26.0.0+

## Configuration

The mapper provides one configuration option:

- **Facade Issuer URL**: The URL to use as the token issuer when requests come through the auth facade

## Installation

### Build

Requirements:
- OpenJDK 11 or higher
- Maven

```bash
mvn clean package
```

### Local Testing with Docker

```bash
mvn clean package && docker run --name keycloak \
  -p 8080:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin \
  -v "$(pwd)/target/keycloak-facade-claim-mapper-jar-with-dependencies.jar:/opt/keycloak/providers/keycloak-facade-claim-mapper-jar-with-dependencies.jar" \
  quay.io/keycloak/keycloak:26.0.0 \
  start-dev
```

### Setup in Keycloak

1. Log into the admin console at http://localhost:8080/admin (username: admin, password: admin)
2. Create or select a realm
3. Create a client scope (e.g., "facade-mapper")
4. Add a mapper to the client scope:
   - Click "Configure a new mapper"
   - Select "Facade Issuer Mapper"
   - Give it a name (e.g., "facade-issuer-override")
   - Configure the "Facade Issuer URL" (e.g., "https://auth-facade.example.com")
5. Add the client scope to your client as a default scope

## How It Works

When a token is requested:

1. The mapper checks for the `X-Auth-Facade-Request: true` header
2. If present and a facade issuer URL is configured, the token's issuer is overridden
3. Otherwise, the token remains unchanged

This ensures that tokens appear to originate from your authentication facade when appropriate, while maintaining standard Keycloak behavior for direct authentication.

## Deployment

Copy the JAR file to Keycloak's providers directory:

```bash
cp target/keycloak-facade-claim-mapper-jar-with-dependencies.jar /opt/keycloak/providers/
```

Then restart Keycloak to load the new provider.

## Use Case

This mapper is ideal for architectures where:
- Keycloak is deployed behind an authentication facade or proxy
- Applications need tokens that reference the facade URL as the issuer
- You want to maintain a consistent issuer URL across different deployment scenarios
package com.agronod.keycloak;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * A Keycloak protocol mapper that overrides the token issuer URL when requests
 * are proxied through an authentication facade.
 * 
 * This mapper detects facade-proxied requests by checking for a specific HTTP
 * header
 * (X-Auth-Facade-Request) and replaces the token's issuer URL with a configured
 * facade URL. This ensures tokens appear to be issued by the facade rather than
 * the underlying Keycloak instance.
 * 
 * Configuration:
 * - facadeIssuerUrl: The URL to use as issuer when facade header is detected
 * 
 * @author Agronod
 */
public class FacadeOIDCProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    // Constants
    public static final String PROVIDER_ID = "oidc-facade-issuer-mapper";
    private static final String FACADE_HEADER_NAME = "X-Auth-Facade-Request";
    private static final String FACADE_HEADER_VALUE = "true";
    private static final String CONFIG_FACADE_ISSUER_URL = "facadeIssuerUrl";

    private static final Logger logger = Logger.getLogger(FacadeOIDCProtocolMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CONFIG_FACADE_ISSUER_URL);
        property.setLabel("Facade Issuer URL");
        property.setHelpText(
                "URL to use as issuer when request comes through auth facade (X-Auth-Facade-Request header is present)");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("");
        configProperties.add(property);
    }

    /**
     * Constructor that logs initialization for debugging.
     */
    public FacadeOIDCProtocolMapper() {
        logger.info("FacadeOIDCProtocolMapper initialized - ready to handle facade issuer overrides");
    }

    /**
     * Returns the configuration properties for this mapper.
     * 
     * @return List of configuration properties
     */
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    /**
     * Returns the display category for this mapper in the Keycloak UI.
     * 
     * @return The category name
     */
    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    /**
     * Returns the display name for this mapper in the Keycloak UI.
     * 
     * @return The display name
     */
    @Override
    public String getDisplayType() {
        return "Facade Issuer Mapper";
    }

    /**
     * Returns the unique provider ID for this mapper.
     * 
     * @return The provider ID
     */
    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * Returns the help text displayed in the Keycloak UI.
     * 
     * @return The help text
     */
    @Override
    public String getHelpText() {
        return "Overrides the token issuer when request comes through an auth facade";
    }

    /**
     * Transforms the access token by potentially overriding the issuer URL.
     * 
     * Algorithm:
     * 1. Check if the request contains the facade header
     * 2. If header is present, retrieve the configured facade issuer URL
     * 3. Validate the URL format
     * 4. Override the token issuer if valid
     * 
     * @param token            The access token to transform
     * @param mappingModel     The protocol mapper configuration
     * @param keycloakSession  The current Keycloak session
     * @param userSession      The user session
     * @param clientSessionCtx The client session context
     * @return The transformed access token
     */
    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
            KeycloakSession keycloakSession,
            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {

        if (isRequestFromFacade(keycloakSession)) {
            String facadeIssuerUrl = mappingModel.getConfig().get(CONFIG_FACADE_ISSUER_URL);

            if (isValidFacadeUrl(facadeIssuerUrl)) {
                String trimmedUrl = facadeIssuerUrl.trim();
                token.issuer(trimmedUrl);
                logger.debug("Overrode token issuer to facade URL: " + trimmedUrl);
            } else if (facadeIssuerUrl != null && !facadeIssuerUrl.trim().isEmpty()) {
                // URL is configured but invalid
                logger.warn("Invalid facade issuer URL configured: " + facadeIssuerUrl);
            }

            logger.debug("Token issuer set to facade URL: " + token.getIssuer());
        } else {
            logger.debug("Request not from facade, using default token issuer: " + token.getIssuer());
        }

        return token;
    }

    /**
     * Checks if the current request came through the authentication facade.
     * 
     * @param keycloakSession The current Keycloak session
     * @return true if the facade header is present with the expected value
     */
    private boolean isRequestFromFacade(KeycloakSession keycloakSession) {
        String headerValue = keycloakSession.getContext()
                .getRequestHeaders()
                .getHeaderString(FACADE_HEADER_NAME);

        return FACADE_HEADER_VALUE.equals(headerValue);
    }

    /**
     * Validates that the provided URL is well-formed and not empty.
     * 
     * @param url The URL to validate
     * @return true if the URL is valid, false otherwise
     */
    private boolean isValidFacadeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            // Validate URL format
            new URL(url.trim());
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
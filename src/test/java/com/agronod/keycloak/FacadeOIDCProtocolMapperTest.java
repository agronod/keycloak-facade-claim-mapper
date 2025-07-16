package com.agronod.keycloak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FacadeOIDCProtocolMapper.
 * 
 * Tests cover:
 * - Provider metadata and configuration
 * - Note: transformAccessToken requires a full Keycloak runtime environment
 * and cannot be easily unit tested without it
 */
class FacadeOIDCProtocolMapperTest {

    private FacadeOIDCProtocolMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FacadeOIDCProtocolMapper();
    }

    /**
     * Test provider metadata methods.
     */
    @Test
    void testProviderMetadata() {
        assertEquals("oidc-facade-issuer-mapper", mapper.getId());
        assertEquals("Facade Issuer Mapper", mapper.getDisplayType());
        assertEquals("Token mapper", mapper.getDisplayCategory());
        assertEquals("Overrides the token issuer when request comes through an auth facade",
                mapper.getHelpText());
    }

    /**
     * Test configuration properties.
     */
    @Test
    void testConfigProperties() {
        List<ProviderConfigProperty> props = mapper.getConfigProperties();

        assertNotNull(props);
        assertEquals(1, props.size());

        ProviderConfigProperty prop = props.get(0);
        assertEquals("facadeIssuerUrl", prop.getName());
        assertEquals("Facade Issuer URL", prop.getLabel());
        assertEquals(ProviderConfigProperty.STRING_TYPE, prop.getType());
        assertEquals("", prop.getDefaultValue());
        assertTrue(prop.getHelpText().contains("X-Auth-Facade-Request"));
    }

    /**
     * Test that we can create a ProtocolMapperModel with config.
     * This verifies our understanding of how Keycloak will use our mapper.
     */
    @Test
    void testProtocolMapperModelUsage() {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setId("test-id");
        model.setName("test-mapper");
        model.setProtocol("openid-connect");
        model.setProtocolMapper("oidc-facade-issuer-mapper");

        Map<String, String> config = new HashMap<>();
        config.put("facadeIssuerUrl", "https://facade.example.com");
        model.setConfig(config);

        assertEquals("https://facade.example.com", model.getConfig().get("facadeIssuerUrl"));
    }

    /**
     * Test that mapper is assignable from expected interfaces.
     */
    @Test
    void testMapperInterfaces() {
        assertTrue(mapper instanceof org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper);
        assertTrue(mapper instanceof org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper);
        assertTrue(mapper instanceof org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper);
    }
}
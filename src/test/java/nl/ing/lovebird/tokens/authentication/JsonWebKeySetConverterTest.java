package nl.ing.lovebird.tokens.authentication;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class JsonWebKeySetConverterTest {

    private static JsonWebKeySet TEST_JWKS;

    private final JsonWebKeySetConverter jsonWebKeySetConverter = new JsonWebKeySetConverter();

    @BeforeClass
    public static void beforeAll() throws Exception {
        RsaJsonWebKey jwk = RsaJwkGenerator.generateJwk(2048);
        jwk.setKeyId(UUID.randomUUID().toString());
        TEST_JWKS = new JsonWebKeySet(jwk);
    }

    @Test
    public void jwksParsingWorks() {
        JsonWebKeySet convertedSet = jsonWebKeySetConverter.convert(TEST_JWKS.toJson());
        assertNotNull(convertedSet);
        assertThat(convertedSet.toJson()).isEqualTo(TEST_JWKS.toJson());
    }

    @Test(expected = IllegalArgumentException.class)
    public void jwksParsingBreaksIfItContainsAPrivateComponent() {
        String jsonWebKeySet = TEST_JWKS.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE);
        jsonWebKeySetConverter.convert(jsonWebKeySet);
    }

    @Test(expected = IllegalArgumentException.class)
    public void jwksParsingBreaksIfItDoesNotContainAProperJWKS() {
        String incorrectJWKS = "This is not a JWKS";
        jsonWebKeySetConverter.convert(incorrectJWKS);
    }

}
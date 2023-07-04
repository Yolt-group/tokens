package nl.ing.lovebird.tokens.authentication;

import com.google.common.collect.ImmutableMap;
import nl.ing.lovebird.tokens.exception.InvalidRequestTokenException;
import nl.ing.lovebird.tokens.exception.InvalidTokenSignatureException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ServiceAuthenticationServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final int VALIDITY_TIME_IN_SEC = 10;
    private static final UUID CLIENT_ID = UUID.randomUUID();

    private static RsaJsonWebKey JWK_SITE_MANAGEMENT;
    private static RsaJsonWebKey JWK_API_GATEWAY_1;
    private static RsaJsonWebKey JWK_API_GATEWAY_2;
    private static Map<String, JsonWebKeySet> serviceJWKSMap;

    private ServiceAuthenticationService serviceAuthenticationService;

    @BeforeClass
    public static void beforeAll() throws Exception {
        JWK_SITE_MANAGEMENT = RsaJwkGenerator.generateJwk(2048);
        JWK_SITE_MANAGEMENT.setKeyId(UUID.randomUUID().toString());
        JWK_API_GATEWAY_1 = RsaJwkGenerator.generateJwk(2048);
        JWK_API_GATEWAY_1.setKeyId(UUID.randomUUID().toString());
        JWK_API_GATEWAY_2 = RsaJwkGenerator.generateJwk(2048);
        JWK_API_GATEWAY_2.setKeyId(UUID.randomUUID().toString());

        serviceJWKSMap = ImmutableMap.of(
                "site-management", new JsonWebKeySet(JWK_SITE_MANAGEMENT),
                "api-gateway", new JsonWebKeySet(JWK_API_GATEWAY_1, JWK_API_GATEWAY_2)
        );
    }

    @Before
    public void before() {
        serviceAuthenticationService = new ServiceAuthenticationService(VALIDITY_TIME_IN_SEC, new ClientTokenRequesterProperties(serviceJWKSMap));
    }

    @Test
    public void authenticationIsSuccessfulWithCorrectRequestToken() throws Exception {
        JwtClaims claims = createClaims("api-gateway", CLIENT_ID.toString());
        JsonWebSignature requestToken = createRequestToken(claims, AlgorithmIdentifiers.RSA_PSS_USING_SHA512, JWK_API_GATEWAY_2.getRsaPrivateKey(), JWK_API_GATEWAY_2.getKeyId());
        String requestTokenJSON = requestToken.getCompactSerialization();

        JwtClaims actualClaims = serviceAuthenticationService.authenticate(requestTokenJSON);

        assertEquals(claims.getClaimsMap(), actualClaims.getClaimsMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticationFailsWhenJWKIsPartOfAnotherService() throws Exception {
        String service = "api-gateway";
        JwtClaims claims = createClaims(service, CLIENT_ID.toString());
        JsonWebSignature requestToken = createRequestToken(claims, AlgorithmIdentifiers.RSA_PSS_USING_SHA512, JWK_SITE_MANAGEMENT.getRsaPrivateKey(), JWK_SITE_MANAGEMENT.getKeyId());
        String requestTokenJSON = requestToken.getCompactSerialization();

        serviceAuthenticationService.authenticate(requestTokenJSON);
    }

    @Test(expected = InvalidTokenSignatureException.class)
    public void authenticationFailsWhenKeyIdIsPartOfAnotherKeyInSameJWKS() throws Exception {
        RSAPrivateKey privateKey = JWK_API_GATEWAY_1.getRsaPrivateKey();
        String keyId = JWK_API_GATEWAY_2.getKeyId();
        JwtClaims claims = createClaims("api-gateway", CLIENT_ID.toString());
        JsonWebSignature requestToken = createRequestToken(claims, AlgorithmIdentifiers.RSA_PSS_USING_SHA512, privateKey, keyId);
        String requestTokenJSON = requestToken.getCompactSerialization();

        serviceAuthenticationService.authenticate(requestTokenJSON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticationFailsWhenJWKIsNotFoundAtAll() throws Exception {
        String keyId = UUID.randomUUID().toString();
        JwtClaims claims = createClaims("site-management", CLIENT_ID.toString());
        JsonWebSignature requestToken = createRequestToken(claims, AlgorithmIdentifiers.RSA_PSS_USING_SHA512, JWK_SITE_MANAGEMENT.getRsaPrivateKey(), keyId);
        String requestTokenJSON = requestToken.getCompactSerialization();

        serviceAuthenticationService.authenticate(requestTokenJSON);
    }

    @Test(expected = IllegalArgumentException.class)
    public void authenticationFailsWhenServiceIsNotRegistered() throws Exception {
        String service = "yoltbank";
        JwtClaims claims = createClaims(service, CLIENT_ID.toString());
        JsonWebSignature requestToken = createRequestToken(claims, AlgorithmIdentifiers.RSA_PSS_USING_SHA512, JWK_SITE_MANAGEMENT.getRsaPrivateKey(), JWK_SITE_MANAGEMENT.getKeyId());
        String requestTokenJSON = requestToken.getCompactSerialization();

        serviceAuthenticationService.authenticate(requestTokenJSON);
    }

    @Test(expected = InvalidRequestTokenException.class)
    public void authenticationFailsWhenJWSIsSignedWithSHA256() throws Exception {
        JwtClaims claims = createClaims("site-management", CLIENT_ID.toString());
        JsonWebSignature requestToken = createRequestToken(claims, AlgorithmIdentifiers.RSA_USING_SHA256, JWK_SITE_MANAGEMENT.getRsaPrivateKey(), JWK_SITE_MANAGEMENT.getKeyId());
        String requestTokenJSON = requestToken.getCompactSerialization();

        serviceAuthenticationService.authenticate(requestTokenJSON);
    }

    private JwtClaims createClaims(String service, String subject) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(service);
        claims.setSubject(subject);
        claims.setJwtId(UUID.randomUUID().toString());
        claims.setIssuedAt(NumericDate.now());
        claims.setExpirationTime(NumericDate.fromSeconds(15 + NumericDate.now().getValue()));
        return claims;
    }

    private JsonWebSignature createRequestToken(JwtClaims claims, String signingAlgorithm, RSAPrivateKey signingKey, String signingKeyId) {
        JsonWebSignature requestToken = new JsonWebSignature();
        requestToken.setPayload(claims.toJson());
        requestToken.setKey(signingKey);
        requestToken.setAlgorithmHeaderValue(signingAlgorithm);
        requestToken.setKeyIdHeaderValue(signingKeyId);
        return requestToken;
    }

}
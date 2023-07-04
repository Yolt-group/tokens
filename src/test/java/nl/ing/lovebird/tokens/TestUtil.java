package nl.ing.lovebird.tokens;

import nl.ing.lovebird.clienttokens.ClientToken;
import org.apache.commons.io.IOUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.EXTRA_CLAIM_CLIENT_GROUP_ID;


public class TestUtil {

    public static final UUID CLIENT_GROUP_ID = UUID.randomUUID();
    public static final ClientToken CLIENT_TOKEN;

    static {
        JwtClaims claims = new JwtClaims();
        claims.setClaim(EXTRA_CLAIM_CLIENT_GROUP_ID, CLIENT_GROUP_ID);
        CLIENT_TOKEN = new ClientToken("serialized", claims);
    }

    public static JsonWebSignature createRequestTokenWithoutJwtId(UUID clientId, Key key) {
        return createToken(null, clientId, NumericDate.now(), key, "");
    }

    public static JsonWebSignature createValidRequestToken(UUID clientId, Key key) {
        return createToken(new UUID(0, 0).toString(), clientId, NumericDate.now(), key, UUID.randomUUID().toString());
    }

    public static JsonWebSignature createValidRequestToken(UUID clientId, Key key, String kid) {
        return createToken(new UUID(0, 0).toString(), clientId, NumericDate.now(), key, kid);
    }

    public static JsonWebSignature createRequestTokenWithoutIssuedAt(UUID clientId, Key privateKey) {
        return createToken(new UUID(0, 0).toString(), clientId, null, privateKey, "");
    }

    public static String getPublicKeyFromPEM() throws IOException {
        return IOUtils.toString(new ClassPathResource("key.pub").getInputStream(), StandardCharsets.UTF_8);
    }

    public static Key getPrivateKey() throws Exception {
        return loadKeystore().getKey("selfsigned", "password".toCharArray());
    }

    public static Key getPublicKey() throws Exception {
        return loadKeystore().getCertificate("selfsigned").getPublicKey();
    }

    private static KeyStore loadKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (InputStream inputStream = new ClassPathResource("keystore.jks").getInputStream()) {
            ks.load(inputStream, "password".toCharArray());
            return ks;
        }
    }

    public static String getHeaderValue(String jwsJSON, String headerName) throws JoseException {
        JsonWebStructure jsonWebStructure = JsonWebStructure.fromCompactSerialization(jwsJSON);
        return jsonWebStructure.getHeaders().getStringHeaderValue(headerName);
    }

    public static JwtConsumer getJweConsumer(String encryptionKey) throws JoseException {
        Map<String, Object> header = new HashMap<>();
        header.put("kty", "oct");
        header.put("k", encryptionKey);
        JsonWebKey encryptionJsonWebKey = JsonWebKey.Factory.newJwk(header);

        return new JwtConsumerBuilder()
                .setDisableRequireSignature()
                .setRequireExpirationTime()
                .setMaxFutureValidityInMinutes(300)
                .setDecryptionKey(encryptionJsonWebKey.getKey())
                .setJweAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, KeyManagementAlgorithmIdentifiers.DIRECT)) // limits acceptable encryption key establishment algorithm(s)
                .setJweContentEncryptionAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512)) // limits acceptable content encryption algorithm(s)
                .build();
    }

    public static JwtConsumer getJwsConsumer(String signatureJwkSecret) throws JoseException {
        JsonWebKey signatureJwk = RsaJsonWebKey.Factory.newPublicJwk(signatureJwkSecret);

        return getJwsConsumer(signatureJwk);
    }

    public static JwtConsumer getJwsConsumer(JsonWebKey signatureJwk) {
        return new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setMaxFutureValidityInMinutes(300)
                .setVerificationKey(signatureJwk.getKey())
                .setJwsAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST, AlgorithmIdentifiers.RSA_PSS_USING_SHA512))
                .build();
    }

    public static JsonWebSignature createToken(@Nullable String jwtId, @Nullable UUID clientId, NumericDate issuedAt, Key key, String kid) {
        JwtClaims claims = new JwtClaims();
        if (clientId != null) {
            claims.setIssuer(clientId.toString());
        }
        if (jwtId != null) {
            claims.setJwtId(jwtId);
        }
        claims.setIssuedAt(issuedAt);
        if (issuedAt != null) {
            issuedAt.addSeconds(10);
            claims.setExpirationTime(issuedAt);
        }

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(key);

        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA512);
        jws.setKeyIdHeaderValue(kid);

        return jws;
    }

    public static String getContentFromFile(String filename, Class<?> aClass) throws IOException {
        return new String(aClass.getResourceAsStream(filename).readAllBytes());
    }

    public static String getDefaultPublicKey() {
        return "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwOMSOOK2qzbQDhiZQdoT\n" +
                "vhi36UWw+Hv7eGKqrKAt2GcU8oiNLpIflWp7vWflfUuuMR959MU6d5m3Z6H3IWOe\n" +
                "A20n+XKhagtHe8biJNNHuZhg5cewHFJVo1YO4xNrBXdCpQuc3eo58MIgoeuImcXk\n" +
                "1wx22toMNUHwOvVyW26IFV9GB3HFl5GqeuBvdzvC+U0ImFqfzoLsD5Z0vI0UW/sK\n" +
                "7WRLXTvaSH7jtApDmL6Q4g+JbvFgvBKbouHbCCN5qbZe1Xh/iJ8VjoTO1VT7UUKL\n" +
                "+mePuPdQRn216LhLNKMBkR9j4WLyvKf0HaLQUlg+QjATfehP3/M87xCUAi70r8Wy\n" +
                "RQIDAQAB\n" +
                "-----END PUBLIC KEY-----";
    }
}

package nl.ing.lovebird.tokens.authentication;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.logging.AuditLogger;
import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.exception.EmptyIssuedAtClaimException;
import nl.ing.lovebird.tokens.exception.EmptyJwtIdClaimException;
import nl.ing.lovebird.tokens.exception.InvalidIssuedAtClaimException;
import nl.ing.lovebird.tokens.exception.InvalidJwtIdClaimException;
import nl.ing.lovebird.tokens.exception.InvalidTokenSignatureException;
import nl.ing.lovebird.tokens.exception.NoClientUuidAsIssuerException;
import nl.ing.lovebird.tokens.verificationkeys.VerificationKey;
import nl.ing.lovebird.tokens.verificationkeys.VerificationKeyService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Security;
import java.time.Instant;
import java.util.UUID;

import static nl.ing.lovebird.tokens.TestUtil.createValidRequestToken;
import static nl.ing.lovebird.tokens.TestUtil.getDefaultPublicKey;
import static nl.ing.lovebird.tokens.TestUtil.getPrivateKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ClientAuthenticationServiceTest {

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final String KEY_ID = "fake_key_id";
    private static final VerificationKey VERIFICATION_KEY = new VerificationKey(CLIENT_ID, KEY_ID, getDefaultPublicKey(), Instant.now());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private ClientAuthenticationService clientAuthenticationService;

    @Mock
    private VerificationKeyService verificationKeyServiceMock;

    @Mock
    private AuditLogger auditLogger;

    @BeforeEach
    void setup() {
        clientAuthenticationService = new ClientAuthenticationService(verificationKeyServiceMock, 5, auditLogger);
    }

    @AfterEach
    void after() {
        verifyNoMoreInteractions(verificationKeyServiceMock);
    }

    @Test
    void shouldSuccessfullyAuthenticateClient() throws Exception {
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);

        JsonWebSignature jws = TestUtil.createValidRequestToken(CLIENT_ID, TestUtil.getPrivateKey());
        String requestToken = jws.getCompactSerialization();

        JwtClaims jwtClaims = clientAuthenticationService.authenticate(requestToken);

        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), CLIENT_ID);
        assertEquals(jwtClaims.getIssuer(), CLIENT_ID.toString());
        verify(auditLogger).logSuccessEvent(any(), any());
    }

    @Test
    void shouldSuccessfullyAuthenticateClientWithPS512() throws Exception {
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);

        JsonWebSignature jws = TestUtil.createValidRequestToken(CLIENT_ID, TestUtil.getPrivateKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA512);
        String requestToken = jws.getCompactSerialization();

        JwtClaims jwtClaims = clientAuthenticationService.authenticate(requestToken);

        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), CLIENT_ID);
        assertEquals(jwtClaims.getIssuer(), CLIENT_ID.toString());
        verify(auditLogger).logSuccessEvent(any(), any());
    }

    @Test
    void shouldFailToAuthenticateClientIfPaddingIsTamperedWith() throws Exception {
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);

        String jwtId = new UUID(0, 0).toString();
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(CLIENT_ID.toString());
        claims.setJwtId(jwtId);
        claims.setIssuedAt(NumericDate.now());

        JsonWebSignature jws = new JsonWebSignature() {
            @Override
            protected byte[] getSignature() {
                byte[] bytes = this.getIntegrity();

                int i = bytes[bytes.length - 1];

                int bit1 = i & 1;// bit at pos0
                int bit2 = (i >> 1) & 1;// bit at pos1

                int mask;
                if (bit1 != bit2) {
                    // Lets create mask 000000011 with ones at specified positions
                    mask = 1 | (1 << 1);
                } else {
                    // Lets create mask 00000001 with ones at specified positions
                    mask = 1;
                }
                bytes[bytes.length - 1] = (byte) (i ^ mask);
                return bytes;
            }
        };
        jws.setPayload(claims.toJson());
        jws.setKey(TestUtil.getPrivateKey());

        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setKeyIdHeaderValue(UUID.randomUUID().toString());

        String requestToken = jws.getCompactSerialization();

        assertThrows(InvalidTokenSignatureException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), CLIENT_ID);
        verify(auditLogger).logErrorEvent(any(), any(), any());
    }

    @Test
    void shouldFailIfTheTokenIsSignedWithAnotherKey() throws JoseException {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);

        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);

        JsonWebSignature jws = TestUtil.createValidRequestToken(CLIENT_ID, rsaJsonWebKey.getRsaPrivateKey());
        String requestToken = jws.getCompactSerialization();

        assertThrows(InvalidTokenSignatureException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), CLIENT_ID);
        verify(auditLogger).logErrorEvent(any(), any(), any());
    }

    @Test
    void shouldFailIfTheTokenHasNoId() throws Exception {
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);

        JsonWebSignature jws = TestUtil.createRequestTokenWithoutJwtId(CLIENT_ID, TestUtil.getPrivateKey());
        String requestToken = jws.getCompactSerialization();

        assertThrows(EmptyJwtIdClaimException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), CLIENT_ID);
        verify(auditLogger).logErrorEvent(any(), any(), any());
    }

    @Test
    void shouldFailIfTheTokenHasAnInvalidId() throws Exception {
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);

        JsonWebSignature jws = TestUtil.createToken("invalidformat", CLIENT_ID, NumericDate.now(),
                TestUtil.getPrivateKey(), "");
        String requestToken = jws.getCompactSerialization();

        assertThrows(InvalidJwtIdClaimException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), CLIENT_ID);
        verify(auditLogger).logErrorEvent(any(), any(), any());
    }

    @Test
    void shouldFailIfTheTokenDoesNotContainIssuedAtClaim() throws Exception {
        final UUID clientUuid = UUID.randomUUID();
        VerificationKey verificationKey = new VerificationKey(clientUuid, KEY_ID, TestUtil.getPublicKeyFromPEM(), Instant.now());
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(verificationKey);

        JsonWebSignature jws = TestUtil.createRequestTokenWithoutIssuedAt(clientUuid, TestUtil.getPrivateKey());
        String requestToken = jws.getCompactSerialization();

        assertThrows(EmptyIssuedAtClaimException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
        verify(verificationKeyServiceMock).getVerificationKey(jws.getKeyIdHeaderValue(), clientUuid);
    }

    @Test
    void shouldFailIfTheTokenWasIssuedALongTimeAgo() throws Exception {
        when(verificationKeyServiceMock.getVerificationKey(any(String.class), any(UUID.class))).thenReturn(VERIFICATION_KEY);
        NumericDate oldDate = NumericDate.now();
        oldDate.addSeconds(-60);

        JsonWebSignature jws = TestUtil.createToken(new UUID(0, 0).toString(), CLIENT_ID, oldDate,
                TestUtil.getPrivateKey(), "");
        String requestToken = jws.getCompactSerialization();

        assertThrows(InvalidIssuedAtClaimException.class,
                () -> clientAuthenticationService.authenticate(requestToken));

        verify(verificationKeyServiceMock).getVerificationKey("", CLIENT_ID);
    }

    @Test
    void getVerificationKey_whenClientIdNull_shouldThrowException() throws Exception {
        JsonWebSignature jws = createValidRequestToken(null, getPrivateKey());
        String requestToken = jws.getCompactSerialization();

        assertThrows(NoClientUuidAsIssuerException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
    }

    @Test
    void getVerificationKey_whenClientIdNotAUUID_shouldThrowException() throws Exception {
        JsonWebSignature jws = createValidRequestToken(null, getPrivateKey());
        jws.setKeyIdHeaderValue("not-a-uuid");
        String requestToken = jws.getCompactSerialization();

        assertThrows(NoClientUuidAsIssuerException.class,
                () -> clientAuthenticationService.authenticate(requestToken));
    }

}

package nl.ing.lovebird.tokens.accesstokens;

import java.security.Security;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.yolt.securityutils.crypto.PasswordKey;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.authentication.ClientAuthenticationService;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonce;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonceRepository;
import nl.ing.lovebird.tokens.clienttokens.ClientTokenService;
import nl.ing.lovebird.tokens.exception.InvalidRequestTokenException;
import nl.ing.lovebird.tokens.exception.NonceAlreadyUsedException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccessTokenServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private AccessTokenService accessTokenService;
    private static final long EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN = 5;
    private static final UUID CLIENT_ID_0 = UUID.randomUUID();
    private static final String ACCESS_TOKEN_ENCRYPTION_SECRET = "Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2IFdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2I";

    private static final String ISSUER_IP_ADDRESS = "127.0.0.1";

    @Mock
    private ClientAuthenticationService clientAuthenticationService;

    @Mock
    private RequestTokenNonceRepository requestTokenNonceRepository;

    @Mock
    private ClientTokenService clientTokenService;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setup() {
        VaultKeys vaultKeys = mock(VaultKeys.class);
        when(vaultKeys.getPassword("cgw-tokens-encryption")).thenReturn(new PasswordKey(ACCESS_TOKEN_ENCRYPTION_SECRET.toCharArray()));
        accessTokenService = new AccessTokenService(clientAuthenticationService, requestTokenNonceRepository, EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, vaultKeys);

        when(requestTokenNonceRepository.getById(any())).thenReturn(Optional.empty());
        when(request.getRemoteAddr()).thenReturn(ISSUER_IP_ADDRESS);
    }

    @After
    public void after() {
        verifyNoMoreInteractions(clientAuthenticationService);
    }

    @Test
    public void shouldSuccessFullyCreateAccessToken() throws Exception {
        // create a requestToken
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        JsonWebSignature requestToken = TestUtil.createValidRequestToken(CLIENT_ID_0, rsaJsonWebKey.getRsaPrivateKey());
        String requestTokenJSON = requestToken.getCompactSerialization();

        // Mock authentication service
        final JwtClaims jwtClaims = JwtClaims.parse(requestToken.getUnverifiedPayload());
        when(clientAuthenticationService.authenticate(anyString())).thenReturn(jwtClaims);

        AccessTokenResponse accessTokenResponse = accessTokenService.getAccessToken(requestTokenJSON, request);

        // Verify output
        verify(clientAuthenticationService).authenticate(requestTokenJSON);
        assertThat(accessTokenResponse.getTokenType()).isEqualTo("Bearer");
        assertNotNull(accessTokenResponse.getAccessToken());
        assertThat(accessTokenResponse.getExpiresIn()).isEqualTo(EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN);
        assertNull(accessTokenResponse.getScopes());

        // Verify access token contents
        String accessTokenCompactSerialization = accessTokenResponse.getAccessToken();

        JwtConsumer jwtConsumer = TestUtil.getJweConsumer(ACCESS_TOKEN_ENCRYPTION_SECRET);
        JwtClaims claims = jwtConsumer.processToClaims(accessTokenCompactSerialization);

        assertEquals("dir", TestUtil.getHeaderValue(accessTokenCompactSerialization, "alg"));
        assertEquals("A256CBC-HS512", TestUtil.getHeaderValue(accessTokenCompactSerialization, "enc"));

        // Verify claims
        assertThat(claims.getSubject()).isEqualTo(CLIENT_ID_0.toString());
        assertThat(claims.getJwtId()).isNotBlank();
    }

    @Test(expected = InvalidRequestTokenException.class)
    public void shouldNotCreateAnAccessTokenWhenAuthenticationFails() throws Exception {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);

        JsonWebSignature requestToken = TestUtil.createValidRequestToken(CLIENT_ID_0, rsaJsonWebKey.getRsaPrivateKey());
        String requestTokenJSON = requestToken.getCompactSerialization();

        when(clientAuthenticationService.authenticate(anyString())).thenThrow(InvalidRequestTokenException.class);

        try {
            accessTokenService.getAccessToken(requestTokenJSON, request);
        } finally {
            verify(clientAuthenticationService).authenticate(requestTokenJSON);
        }
    }

    @Test(expected = NonceAlreadyUsedException.class)
    public void shouldFailBecauseOfANonceIsAlreadyUsed() throws Exception {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);

        final UUID nonce = UUID.randomUUID();
        JsonWebSignature requestToken = TestUtil.createToken(nonce.toString(), CLIENT_ID_0, NumericDate.now(), rsaJsonWebKey.getRsaPrivateKey(), "fake_kid");
        final JwtClaims jwtClaims = JwtClaims.parse(requestToken.getUnverifiedPayload());
        String requestTokenJSON = requestToken.getCompactSerialization();

        when(clientAuthenticationService.authenticate(anyString())).thenReturn(jwtClaims);
        when(requestTokenNonceRepository.getById(any())).thenReturn(Optional.of(new RequestTokenNonce(nonce)));

        try {
            accessTokenService.getAccessToken(requestTokenJSON, request);
        } finally {
            verify(clientAuthenticationService).authenticate(requestTokenJSON);
        }
    }
}

package nl.ing.lovebird.tokens.clienttokens;

import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonce;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonceRepository;
import nl.ing.lovebird.tokens.authentication.ServiceAuthenticationService;
import nl.ing.lovebird.tokens.exception.InvalidRequestTokenException;
import nl.ing.lovebird.tokens.exception.NonceAlreadyUsedException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Security;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientTokenRequestServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final long EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN = 5;
    private static final UUID CLIENT_ID_0 = UUID.randomUUID();
    private static final UUID USER_ID_0 = UUID.randomUUID();

    @Mock
    private ServiceAuthenticationService serviceAuthenticationService;

    @Mock
    private RequestTokenNonceRepository requestTokenNonceRepository;

    @Mock
    private ClientTokenService clientTokenService;

    private ClientTokenRequestService clientTokenRequestService;

    @Before
    public void setup() {
        clientTokenRequestService = new ClientTokenRequestService(clientTokenService, serviceAuthenticationService, requestTokenNonceRepository, EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN);

        when(requestTokenNonceRepository.getById(any())).thenReturn(Optional.empty());
    }

    @Test
    public void shouldSuccessfullyRequestClientTokenLegacy() throws Exception {
        String jwtId = UUID.randomUUID().toString();
        String kid = UUID.randomUUID().toString();
        String requester = "site-management";
        String expectedClientToken = "ClientToken";

        String requestTokenJSON = createRequestToken(requester, jwtId, kid, CLIENT_ID_0.toString());

        // Mock services
        when(serviceAuthenticationService.authenticate(requestTokenJSON)).thenReturn(createClaims(jwtId, requester, CLIENT_ID_0.toString()));
        when(clientTokenService.createClientToken(CLIENT_ID_0, EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, requester)).thenReturn(expectedClientToken);

        // call the service
        ClientTokenResponseDTO clientTokenResponseDTO = clientTokenRequestService.requestClientToken(requestTokenJSON);

        // Verify
        assertEquals(expectedClientToken, clientTokenResponseDTO.getClientToken());
        assertEquals(EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, clientTokenResponseDTO.getExpiresIn());
    }

    @Test
    public void shouldSuccessfullyRequestClientToken() throws Exception {
        String jwtId = UUID.randomUUID().toString();
        String kid = UUID.randomUUID().toString();
        String requester = "site-management";
        String expectedClientToken = "ClientToken";

        String subject = "client:" + CLIENT_ID_0;
        String requestTokenJSON = createRequestToken(requester, jwtId, kid, subject);

        // Mock services
        when(serviceAuthenticationService.authenticate(requestTokenJSON)).thenReturn(createClaims(jwtId, requester, subject));
        when(clientTokenService.createClientToken(CLIENT_ID_0, EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, requester)).thenReturn(expectedClientToken);

        // call the service
        ClientTokenResponseDTO clientTokenResponseDTO = clientTokenRequestService.requestClientToken(requestTokenJSON);

        // Verify
        assertEquals(expectedClientToken, clientTokenResponseDTO.getClientToken());
        assertEquals(EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, clientTokenResponseDTO.getExpiresIn());
    }

    @Test
    public void shouldSuccessfullyRequestClientGroupToken() throws Exception {
        String jwtId = UUID.randomUUID().toString();
        String kid = UUID.randomUUID().toString();
        String requester = "site-management";
        String expectedClientToken = "ClientToken";

        String subject = "group:" + CLIENT_ID_0;
        String requestTokenJSON = createRequestToken(requester, jwtId, kid, subject);

        // Mock services
        when(serviceAuthenticationService.authenticate(requestTokenJSON)).thenReturn(createClaims(jwtId, requester, subject));
        when(clientTokenService.createClientGroupToken(CLIENT_ID_0, EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, requester)).thenReturn(expectedClientToken);

        // call the service
        ClientTokenResponseDTO clientTokenResponseDTO = clientTokenRequestService.requestClientToken(requestTokenJSON);

        // Verify
        assertEquals(expectedClientToken, clientTokenResponseDTO.getClientToken());
        assertEquals(EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, clientTokenResponseDTO.getExpiresIn());
    }

    @Test
    public void shouldSuccessfullyRequestClientUserToken() throws Exception {
        String jwtId = UUID.randomUUID().toString();
        String kid = UUID.randomUUID().toString();
        String requester = "site-management";
        String expectedClientToken = "ClientToken";

        String subject = "client-user:" + CLIENT_ID_0 + "," + USER_ID_0;
        String requestTokenJSON = createRequestToken(requester, jwtId, kid, subject);

        // Mock services
        when(serviceAuthenticationService.authenticate(requestTokenJSON)).thenReturn(createClaims(jwtId, requester, subject));
        when(clientTokenService.createClientUserToken(CLIENT_ID_0, USER_ID_0, EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, requester)).thenReturn(expectedClientToken);

        // call the service
        ClientTokenResponseDTO clientTokenResponseDTO = clientTokenRequestService.requestClientToken(requestTokenJSON);

        // Verify
        assertEquals(expectedClientToken, clientTokenResponseDTO.getClientToken());
        assertEquals(EXPIRATION_TIME_IN_SEC_ACCESS_TOKEN, clientTokenResponseDTO.getExpiresIn());
    }

    @Test(expected = InvalidRequestTokenException.class)
    public void shouldNotCreateAnAccessTokenWhenAuthenticationFails() throws Exception {
        String requestTokenJSON = "fakeRequestToken";
        when(serviceAuthenticationService.authenticate(requestTokenJSON)).thenThrow(InvalidRequestTokenException.class);

        clientTokenRequestService.requestClientToken(requestTokenJSON);
    }

    @Test(expected = NonceAlreadyUsedException.class)
    public void shouldFailBecauseOfANonceIsAlreadyUsed() throws Exception {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);

        final UUID nonce = UUID.randomUUID();
        JsonWebSignature requestToken = TestUtil.createToken(nonce.toString(), CLIENT_ID_0, NumericDate.now(), rsaJsonWebKey.getRsaPrivateKey(), "fake_kid");
        final JwtClaims jwtClaims = JwtClaims.parse(requestToken.getUnverifiedPayload());
        String requestTokenJSON = requestToken.getCompactSerialization();

        when(serviceAuthenticationService.authenticate(anyString())).thenReturn(jwtClaims);
        when(requestTokenNonceRepository.getById(any())).thenReturn(Optional.of(new RequestTokenNonce(nonce)));

        clientTokenRequestService.requestClientToken(requestTokenJSON);
    }

    private String createRequestToken(String requester, String jwtId, String kid, String subject) throws Exception{
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);

        JsonWebSignature requestToken = new JsonWebSignature();
        requestToken.setPayload(createClaims(jwtId, requester, subject).toJson());
        requestToken.setKey(rsaJsonWebKey.getRsaPrivateKey());
        requestToken.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA512);
        requestToken.setKeyIdHeaderValue(kid);

        return requestToken.getCompactSerialization();
    }

    private JwtClaims createClaims(String jwtId, String requester, String subject) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(requester);
        claims.setSubject(subject);
        claims.setJwtId(jwtId);
        claims.setIssuedAt(NumericDate.now());
        claims.setExpirationTime(NumericDate.fromSeconds(15 + NumericDate.now().getValue()));

        return claims;
    }
}

package nl.ing.lovebird.tokens.accesstokens;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.tokens.authentication.ClientAuthenticationService;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonce;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonceRepository;
import nl.ing.lovebird.tokens.exception.CouldNotCreateAccessTokenException;
import nl.ing.lovebird.tokens.exception.NonceAlreadyUsedException;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class AccessTokenService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";
    private static final String TOKEN_HEADER_ENCRYPTION_KEY_TYPE = "kty";
    private static final String TOKEN_HEADER_ENCRYPTION_KEY = "k";
    private static final String TOKEN_HEADER_ENCRYPTION_KEY_TYPE_OCTET_SEQUENCE = "oct";
    private static final String TOKEN_EXTRA_CLAIM_SUBJECT_IP = "sub-ip";

    private final ClientAuthenticationService clientAuthenticationService;
    private final RequestTokenNonceRepository requestTokenNonceRepository;

    private final long expirationTimeInSec;
    private final String encryptionSecret;

    public AccessTokenService(ClientAuthenticationService clientAuthenticationService,
                              RequestTokenNonceRepository requestTokenNonceRepository,
                              @Value("${service.tokens.access-token.expiration-time-in-sec}") long expirationTimeInSec,
                              VaultKeys vaultKeys) {
        this.clientAuthenticationService = clientAuthenticationService;
        this.requestTokenNonceRepository = requestTokenNonceRepository;
        this.expirationTimeInSec = expirationTimeInSec;
        this.encryptionSecret = new String(vaultKeys.getPassword("cgw-tokens-encryption").getPassword());
    }

    AccessTokenResponse getAccessToken(String requestToken, HttpServletRequest request) throws MalformedClaimException {
        JwtClaims requestTokenClaims = clientAuthenticationService.authenticate(requestToken);

        UUID nonceUuid = UUID.fromString(requestTokenClaims.getJwtId());
        checkNonce(nonceUuid);
        saveNonce(nonceUuid);
        UUID clientId = UUID.fromString(requestTokenClaims.getIssuer());
        String originIp = request.getRemoteAddr();

        if (StringUtils.isEmpty(originIp)) {
            throw new CouldNotCreateAccessTokenException("Origin IP cannot be determined");
        }

        String accessToken = createAccessToken(clientId, originIp);

        return new AccessTokenResponse(accessToken, BEARER_TOKEN_TYPE, expirationTimeInSec, null);
    }

    private void saveNonce(UUID nonceUuid) {
        requestTokenNonceRepository.save(new RequestTokenNonce(nonceUuid));
    }

    private void checkNonce(UUID nonceUuid) {
        requestTokenNonceRepository.getById(nonceUuid).ifPresent(requestTokenNonce -> {
            throw new NonceAlreadyUsedException();
        });
    }

    private String createAccessToken(UUID clientId, String originIp) {
        try {
            // The shared secret or shared symmetric key represented as a octet sequence JSON Web Key (JWK)
            Map<String, Object> header = new HashMap<>();
            header.put(TOKEN_HEADER_ENCRYPTION_KEY_TYPE, TOKEN_HEADER_ENCRYPTION_KEY_TYPE_OCTET_SEQUENCE);
            header.put(TOKEN_HEADER_ENCRYPTION_KEY, encryptionSecret);
            JsonWebKey jwk = JsonWebKey.Factory.newJwk(header);

            JwtClaims claims = new JwtClaims();
            claims.setSubject(clientId.toString());
            claims.setIssuedAt(NumericDate.now());
            claims.setExpirationTime(determineExpirationDate(expirationTimeInSec));
            claims.setJwtId(UUID.randomUUID().toString());
            claims.setClaim(TOKEN_EXTRA_CLAIM_SUBJECT_IP, originIp);

            JsonWebEncryption jwe = new JsonWebEncryption();
            jwe.setPlaintext(claims.toJson());
            jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);
            jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512);
            jwe.setKey(jwk.getKey());

            return jwe.getCompactSerialization();
        } catch (JoseException e) {
            // "should never happen", anyways, it is something unrecoverable.
            throw new CouldNotCreateAccessTokenException("Unable to create access token for client " + clientId, e);
        }
    }

    private NumericDate determineExpirationDate(long expirationTimeInSec) {
        NumericDate expirationDate = NumericDate.now();
        expirationDate.addSeconds(expirationTimeInSec);
        // Add 5 more seconds since we also communicate the expiration-in-seconds as part of the a response (oauth 2 spec).
        // The client should be able to rely on that, so the token actually expires a bit later.
        // For example, of expiration-in-seconds = 60. The token should still be valid for 60 seconds the moment the client
        // sees this response. Therefore, the actual token contains a value that is valid for a bit longer to adjust for
        // network latencies etc.
        expirationDate.addSeconds(5L);
        return expirationDate;
    }
}

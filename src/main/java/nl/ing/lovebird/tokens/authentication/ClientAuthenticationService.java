package nl.ing.lovebird.tokens.authentication;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.logging.AuditLogger;
import nl.ing.lovebird.logging.SemaEventLogger;
import nl.ing.lovebird.tokens.authentication.sema.ClientRequestTokenInvalidSemaEvent;
import nl.ing.lovebird.tokens.authentication.sema.ClientRequestTokenParsingFailedSemaEvent;
import nl.ing.lovebird.tokens.exception.InvalidRequestTokenException;
import nl.ing.lovebird.tokens.exception.NoClientUuidAsIssuerException;
import nl.ing.lovebird.tokens.publickeys.PublicKeyParser;
import nl.ing.lovebird.tokens.verificationkeys.VerificationKey;
import nl.ing.lovebird.tokens.verificationkeys.VerificationKeyService;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.UUID;

/**
 * This service is responsible for authenticating clients by request tokens.
 * They will be authenticated against the public verification key that they have provided in our portal.
 * The request token has to comply with specific rules like the algorithm constraint and others which can be found in the base class.
 */
@Slf4j
@Service
public class ClientAuthenticationService implements RequestTokenAuthenticator {

    private static final AlgorithmConstraints JWS_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST,
            AlgorithmIdentifiers.RSA_USING_SHA256, AlgorithmIdentifiers.RSA_USING_SHA384, AlgorithmIdentifiers.RSA_USING_SHA512,
            AlgorithmIdentifiers.RSA_PSS_USING_SHA512);

    private final VerificationKeyService verificationKeyService;
    private final RequestTokenValidator requestTokenValidator;
    private final AuditLogger auditLogger;

    @Autowired
    public ClientAuthenticationService(VerificationKeyService verificationKeyService,
                                       @Value("${request-token.validity-in-sec}") int validityTimeInSec, AuditLogger auditLogger) {
        this.requestTokenValidator = new RequestTokenValidator(validityTimeInSec, JWS_ALGORITHM_CONSTRAINTS);
        this.verificationKeyService = verificationKeyService;
        this.auditLogger = auditLogger;
    }

    public JwtClaims authenticate(String requestToken) {
        // Attempt to parse the requestToken into a JWS.
        JsonWebSignature requestTokenJws;
        try {
            requestTokenJws = (JsonWebSignature) JsonWebSignature.fromCompactSerialization(requestToken);
        } catch (JoseException e) {
            String msg = String.format("Cannot parse requestToken into a JWS %s", requestToken);
            log.error(msg, e);
            auditLogger.logErrorEvent(msg, new AuthenticationAuditEvent(requestToken, null), e);
            AuthenticationAttemptMetrics.incrementErrorAuthentication();
            SemaEventLogger.log(new ClientRequestTokenParsingFailedSemaEvent(msg));
            throw new IllegalArgumentException(e);
        }

        // Get basic info from the jws
        String jwsAlgorithm = requestTokenJws.getAlgorithmHeaderValue();
        String kid = requestTokenJws.getKeyIdHeaderValue();
        UUID clientId;
        try {
            clientId = getClientId(requestTokenJws);
        } catch (MalformedClaimException | InvalidJwtException e) {
            String msg = "Unable to retrieve verification key for a client on request token";
            log.error(msg, e);
            AuditLogger.logError(msg, new AuthenticationAuditEvent(requestToken, null), e);
            AuthenticationAttemptMetrics.incrementErrorAuthentication(jwsAlgorithm);
            SemaEventLogger.log(new ClientRequestTokenInvalidSemaEvent(msg, kid));
            throw new InvalidRequestTokenException("Could not get unverified payload from web token", e);
        }

        // Attempt to get the right public key from the client id and the kid header.
        final VerificationKey verificationKey;
        try {
            verificationKey = verificationKeyService.getVerificationKey(kid, clientId);
        } catch (RuntimeException e) {
            String msg = "Unable to retrieve verification key for a client on request token";
            log.error(msg, e);
            auditLogger.logErrorEvent(msg, new AuthenticationAuditEvent(requestToken, null), e);
            AuthenticationAttemptMetrics.incrementErrorAuthentication(clientId, jwsAlgorithm);
            SemaEventLogger.log(new ClientRequestTokenInvalidSemaEvent(msg, kid, clientId));
            throw e;
        }

        // Verify the requestToken with the right public key and return the claims.
        try {
            PublicKey publicKey = PublicKeyParser.parse(verificationKey.getVerificationKey());
            JwtClaims jwtClaims = requestTokenValidator.validateRequestToken(requestToken, publicKey);
            AuthenticationAttemptMetrics.incrementSuccessFullAuthentication(verificationKey.getClientId(), jwsAlgorithm);
            auditLogger.logSuccessEvent("Client successfully authenticated",
                    new AuthenticationAuditEvent(requestToken, verificationKey.getVerificationKey()));
            return jwtClaims;
        } catch (RuntimeException e) {
            AuthenticationAttemptMetrics.incrementErrorAuthentication(clientId, jwsAlgorithm);
            String msg = String.format("Request token is invalid for client %s", verificationKey.getClientId());
            log.error(msg, e);
            auditLogger.logErrorEvent(msg, new AuthenticationAuditEvent(requestToken, verificationKey.getVerificationKey()), e);
            SemaEventLogger.log(new ClientRequestTokenInvalidSemaEvent(msg, kid, clientId));
            throw e;
        }
    }

    private static UUID getClientId(final JsonWebSignature jsonWebSignature) throws InvalidJwtException, MalformedClaimException {
        final JwtClaims jwtClaims = JwtClaims.parse(jsonWebSignature.getUnverifiedPayload());
        final String jwtIssuer = jwtClaims.getIssuer();
        if (StringUtils.isBlank(jwtIssuer)) {
            throw new NoClientUuidAsIssuerException();
        }
        try {
            return UUID.fromString(jwtIssuer);
        } catch (IllegalArgumentException e) {
            throw new NoClientUuidAsIssuerException(e);
        }
    }
}

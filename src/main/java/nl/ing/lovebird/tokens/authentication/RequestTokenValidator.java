package nl.ing.lovebird.tokens.authentication;

import nl.ing.lovebird.tokens.exception.InvalidRequestTokenException;
import nl.ing.lovebird.tokens.exception.InvalidTokenSignatureException;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.InvalidJwtSignatureException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import java.security.Key;

class RequestTokenValidator {

    private final RequestTokenJwtIdValidator requestTokenJwtIdValidator;
    private final RequestTokenRequireIssuerValidator requestTokenRequireIssuerValidator;
    private final RequestTokenIssuedAtValidator requestTokenIssuedAtValidator;
    private final AlgorithmConstraints jwsAlgorithmConstraints;

    RequestTokenValidator(int validityTimeInSec, AlgorithmConstraints jwsAlgorithmConstraints) {
        this.requestTokenJwtIdValidator = new RequestTokenJwtIdValidator();
        this.requestTokenRequireIssuerValidator = new RequestTokenRequireIssuerValidator();
        this.requestTokenIssuedAtValidator = new RequestTokenIssuedAtValidator(validityTimeInSec);
        this.jwsAlgorithmConstraints = jwsAlgorithmConstraints;
    }

    private JwtConsumerBuilder createJwtConsumerBuilder() {
        return new JwtConsumerBuilder()
                .registerValidator(requestTokenJwtIdValidator)
                .registerValidator(requestTokenRequireIssuerValidator)
                // The request token is only valid for 'x'-seconds after it has been issued.
                .registerValidator(requestTokenIssuedAtValidator)
                .setJwsAlgorithmConstraints(this.jwsAlgorithmConstraints);
    }

    JwtClaims validateRequestToken(String requestToken, Key verificationKey) {
        try {
            JwtConsumer jwtConsumer = createJwtConsumerBuilder()
                    .setVerificationKey(verificationKey)
                    .build();
            //  Validate the JWT
            return jwtConsumer.processToClaims(requestToken);
        } catch (InvalidJwtSignatureException e) {
            throw new InvalidTokenSignatureException(e);
        } catch (InvalidJwtException e) {
            // Is some generic exception that might contain a message from validators wrapped in the exception.
            // Try to make the exception a bit more specific.
            // Too bad, but exceptions cannot be thrown in validators themselves.
            throw createSpecificRuntimeException(e);
        }
    }

    private RuntimeException createSpecificRuntimeException(InvalidJwtException e) {
        if (!StringUtils.isEmpty(e.getMessage())) {
            for (RequestTokenValidationError validationError : RequestTokenValidationError.values()) {
                if (e.getMessage().contains(validationError.getMessage())) {
                    throw validationError.getRuntimeException();
                }
            }
        }
        throw new InvalidRequestTokenException("Request token is invalid", e);
    }
}
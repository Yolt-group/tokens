package nl.ing.lovebird.tokens.authentication;

import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
class RequestTokenJwtIdValidator implements Validator {

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {

        JwtClaims jwtClaims = jwtContext.getJwtClaims();

        if (StringUtils.isEmpty(jwtClaims.getJwtId())) {
            log.info("The jwt id (jti) is a required standard claim.");
            return RequestTokenValidationError.JTI_CLAIM_EMPTY.getMessage();
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(jwtClaims.getJwtId());
        } catch (IllegalArgumentException e) {
            log.info("Invalid value for jti, should be a uuid nonce.");
            return RequestTokenValidationError.JTI_CLAIM_INVALID.getMessage();
        }
        return null;
    }
}

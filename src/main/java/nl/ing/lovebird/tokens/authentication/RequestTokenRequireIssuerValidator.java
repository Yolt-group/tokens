package nl.ing.lovebird.tokens.authentication;

import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;
import org.springframework.util.StringUtils;

import static nl.ing.lovebird.tokens.authentication.RequestTokenValidationError.ISS_CLAIM_EMPTY;

@Slf4j
class RequestTokenRequireIssuerValidator implements Validator {
    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        JwtClaims jwtClaims = jwtContext.getJwtClaims();

        if (StringUtils.isEmpty(jwtClaims.getIssuer())) {
            log.info("The issuer (iss) is a required standard claim.");
            return ISS_CLAIM_EMPTY.getMessage();
        }
        return null;
    }
}

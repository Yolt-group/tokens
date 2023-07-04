package nl.ing.lovebird.tokens.authentication;

import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwt.consumer.Validator;

@Slf4j
class RequestTokenIssuedAtValidator implements Validator {

    private final int validityTimeInSec;

    static final int ALLOWED_CLOCK_SKEW_IN_SECONDS = 5;

    RequestTokenIssuedAtValidator(int validityTimeInSec) {
        this.validityTimeInSec = validityTimeInSec;
    }

    @Override
    public String validate(JwtContext jwtContext) throws MalformedClaimException {
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        NumericDate issuedAt = jwtClaims.getIssuedAt();

        if (issuedAt == null) {
            return RequestTokenValidationError.IAT_CLAIM_EMPTY.getMessage();
        }

        NumericDate nowIncludingAllowedClockSkew = NumericDate.now();
        nowIncludingAllowedClockSkew.addSeconds(ALLOWED_CLOCK_SKEW_IN_SECONDS);
        if (issuedAt.isAfter(nowIncludingAllowedClockSkew)) {
            log.info("The Issued At Time (iat={}) claim value cannot be after the current numeric date.", issuedAt); //NOSHERIFF
            return RequestTokenValidationError.IAT_CLAIM_CANNOT_BE_IN_FUTURE.getMessage();
        }

        NumericDate issuedAtExpiryTime = NumericDate.now();
        // e.g. : 10 seconds in the past. Means that a tokens issued 11 seconds in the past is expired.
        issuedAtExpiryTime.addSeconds(-validityTimeInSec);
        // e.g. : 15 seconds in the past (-10 - 5). Means that a token issued 15 seconds in the past is expired.
        issuedAtExpiryTime.addSeconds(-ALLOWED_CLOCK_SKEW_IN_SECONDS);
        if (issuedAt.isBefore(issuedAtExpiryTime)) {
            log.info("The Issued At Time (iat={}) claim value cannot be before {}. The token is not valid anymore.",
                    issuedAt.toString(), issuedAtExpiryTime.toString()); //NOSHERIFF
            return RequestTokenValidationError.IAT_CLAIM_EXPIRED.getMessage();
        }
        return null;
    }
}

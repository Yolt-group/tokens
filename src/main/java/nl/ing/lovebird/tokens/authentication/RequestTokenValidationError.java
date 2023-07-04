package nl.ing.lovebird.tokens.authentication;

import lombok.Getter;
import nl.ing.lovebird.tokens.exception.*;

@Getter
enum RequestTokenValidationError {

    IAT_CLAIM_EMPTY("The issued at (iat) claim is empty.", new EmptyIssuedAtClaimException()),
    IAT_CLAIM_CANNOT_BE_IN_FUTURE("The issued at (iat) claim cannot be in the future.", new InvalidIssuedAtClaimException()),
    IAT_CLAIM_EXPIRED("The issued at (iat) claim is expired.", new InvalidIssuedAtClaimException()),
    ISS_CLAIM_EMPTY("The issued at (iat) claim is empty.", new EmptyIssuerClaimException()),
    JTI_CLAIM_EMPTY("The jwt id (jti) claim is empty.", new EmptyJwtIdClaimException()),
    JTI_CLAIM_INVALID("The jwt id (jti) claim has an invalid format.", new InvalidJwtIdClaimException());

    private final String message;
    private final RuntimeException runtimeException;

    RequestTokenValidationError(String message, RuntimeException runtimeException) {
        this.runtimeException = runtimeException;
        this.message = message;
    }


}

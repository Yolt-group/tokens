package nl.ing.lovebird.tokens.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoClientUuidAsIssuerException extends RuntimeException {
    public NoClientUuidAsIssuerException(Throwable cause) {
        super(cause);
    }
}

package nl.ing.lovebird.tokens.exception;

public class InvalidTokenSignatureException extends RuntimeException{
    public InvalidTokenSignatureException(Throwable cause) {
        super(cause);
    }
}

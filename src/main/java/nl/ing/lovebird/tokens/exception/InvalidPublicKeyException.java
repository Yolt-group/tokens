package nl.ing.lovebird.tokens.exception;

public class InvalidPublicKeyException extends RuntimeException{
    public InvalidPublicKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}

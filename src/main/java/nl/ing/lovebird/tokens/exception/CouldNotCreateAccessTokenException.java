package nl.ing.lovebird.tokens.exception;


public class CouldNotCreateAccessTokenException extends RuntimeException {
    public CouldNotCreateAccessTokenException(String message) {
        super(message);
    }
    public CouldNotCreateAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }

}

package nl.ing.lovebird.tokens.exception;

public class InvalidRequestTokenException extends RuntimeException{

    public InvalidRequestTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

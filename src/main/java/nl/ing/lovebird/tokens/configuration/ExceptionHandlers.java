package nl.ing.lovebird.tokens.configuration;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.errorhandling.ExceptionHandlingService;
import nl.ing.lovebird.tokens.clients.ClientAlreadyExistsException;
import nl.ing.lovebird.tokens.clients.ClientGroupAlreadyExistsException;
import nl.ing.lovebird.tokens.clients.ClientGroupNotFoundException;
import nl.ing.lovebird.tokens.clients.ClientNotFoundException;
import nl.ing.lovebird.tokens.exception.*;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolationException;

import static nl.ing.lovebird.tokens.configuration.ErrorConstants.*;

@ControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionHandlers {

    private final ExceptionHandlingService service;

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected ErrorDTO handle(CouldNotCreateAccessTokenException ex) {
        return service.logAndConstruct(CREATE_ACCESS_TOKEN_FAILED, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    protected ErrorDTO handle(InvalidPublicKeyException ex) {
        return service.logAndConstruct(INVALID_PUBLIC_KEY, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(UnsupportedGrantTypeException ex) {
        return service.logAndConstruct(Level.WARN, INVALID_GRANT_TYPE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(InvalidRequestTokenException ex) {
        return service.logAndConstruct(INVALID_REQUEST_TOKEN, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(NoClientUuidAsIssuerException ex) {
        return service.logAndConstruct(Level.WARN, NO_CLIENT_UUID, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    protected ErrorDTO handle(InvalidTokenSignatureException ex) {
        return service.logAndConstruct(INVALID_SIGNATURE, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(EmptyJwtIdClaimException ex) {
        return service.logAndConstruct(Level.WARN, EMPTY_JWT_ID, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(EmptyIssuerClaimException ex) {
        return service.logAndConstruct(Level.WARN, EMPTY_JWT_ISSUER, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(EmptyIssuedAtClaimException ex) {
        return service.logAndConstruct(Level.WARN, EMPTY_JWT_ISSUED_AT, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(InvalidJwtIdClaimException ex) {
        return service.logAndConstruct(Level.WARN, INVALID_JWT_ID, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(InvalidIssuedAtClaimException ex) {
        return service.logAndConstruct(Level.WARN, INVALID_ISSUED_AT, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(IllegalArgumentException ex) {
        return service.logAndConstruct(INVALID_REQUEST_PARAMETERS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    protected ErrorDTO handle(VerificationKeyAlreadyStoredException ex) {
        return service.logAndConstruct(VERIFICATION_KEY_ALREADY_EXISTS, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    protected ErrorDTO handle(VerificationKeyNotFoundException ex) {
        return service.logAndConstruct(NO_VERIFICATION_KEY, ex);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return service.logAndConstruct(Level.WARN, INVALID_CONTENT_TYPE, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO handle(ConstraintViolationException ex) {
        return service.logAndConstruct(INVALID_REQUEST_PARAMETERS, ex);
    }

    @ExceptionHandler(NameIsAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(NameIsAlreadyUsedException ex) {
        return service.logAndConstruct(KEYPAIR_NAME_COLLISION, ex);
    }

    @ExceptionHandler(ClientNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientNotFoundException ex) {
        return service.logAndConstruct(CLIENT_NOT_FOUND, ex);
    }

    @ExceptionHandler(ClientGroupNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupNotFoundException ex) {
        return service.logAndConstruct(CLIENTGROUP_NOT_FOUND, ex);
    }

    @ExceptionHandler(ClientAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientAlreadyExistsException ex) {
        return service.logAndConstruct(CLIENT_EXISTS, ex);
    }

    @ExceptionHandler(ClientGroupAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    protected ErrorDTO handle(ClientGroupAlreadyExistsException ex) {
        return service.logAndConstruct(CLIENTGROUP_EXISTS, ex);
    }

    @ExceptionHandler(KeypairNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    protected ErrorDTO handle(KeypairNotFoundException ex) {
        return service.logAndConstruct(KEYPAIR_NOT_FOUND, ex);
    }
}

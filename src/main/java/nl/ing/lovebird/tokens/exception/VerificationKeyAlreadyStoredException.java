package nl.ing.lovebird.tokens.exception;

import java.util.UUID;

public class VerificationKeyAlreadyStoredException extends RuntimeException {

    public VerificationKeyAlreadyStoredException(UUID clientId, String keyId) {
        super("Verification key is already stored for clientId " + clientId + " and keyId " + keyId);
    }
}

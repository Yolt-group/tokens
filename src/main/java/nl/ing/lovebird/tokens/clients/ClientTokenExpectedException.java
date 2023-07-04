package nl.ing.lovebird.tokens.clients;

import java.util.UUID;

public class ClientTokenExpectedException extends RuntimeException {
    public ClientTokenExpectedException(UUID clientId) {
        super("Expected a client token with client id " + clientId);
    }
}

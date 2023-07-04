package nl.ing.lovebird.tokens.clients;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(final UUID clientId) {
        super("No client found for id: " + clientId.toString());
    }
}

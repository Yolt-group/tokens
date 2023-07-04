package nl.ing.lovebird.tokens.clients;

import java.util.UUID;

public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(final UUID clientId) {
        super("Client already exists with id: " + clientId.toString());
    }
}

package nl.ing.lovebird.tokens.clients;

import java.util.UUID;

public class ClientGroupAlreadyExistsException extends RuntimeException {

    public ClientGroupAlreadyExistsException(final UUID clientId) {
        super("Client group already exists with id: " + clientId.toString());
    }
}

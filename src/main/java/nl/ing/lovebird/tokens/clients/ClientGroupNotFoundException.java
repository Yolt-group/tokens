package nl.ing.lovebird.tokens.clients;

import java.util.UUID;

public class ClientGroupNotFoundException extends RuntimeException {

    public ClientGroupNotFoundException(final UUID clientGroupId) {
        super("No client-group found for id: " + clientGroupId.toString());
    }
}

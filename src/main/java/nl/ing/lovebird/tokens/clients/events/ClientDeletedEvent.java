package nl.ing.lovebird.tokens.clients.events;

import lombok.Value;

import java.util.UUID;

@Value
public class ClientDeletedEvent {
    UUID clientId;
}

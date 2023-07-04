package nl.ing.lovebird.tokens.verificationkeys.events;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class RequestTokenPublicKeyEvent {

    @NotNull
    RequestTokenPublicKeyEvent.Action action;

    @NotNull
    UUID clientId;

    @NotNull
    String keyId;

    @NotNull
    String requestTokenPublicKey;

    @NotNull
    LocalDateTime created;

    public enum Action {
        ADD, DELETE, UPDATE
    }
}

package nl.ing.lovebird.tokens.clienttokens;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ClientUserDTO {

    private UUID clientId;
    private UUID clientUserId;
    private UUID userId;
    private boolean blocked;
    private String blockedBy;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private boolean oneOffAISUser;
}

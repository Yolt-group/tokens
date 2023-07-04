package nl.ing.lovebird.tokens.clients.dto;

import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
public class ClientGroupDTO {
    @NotNull
    private UUID clientGroupId;
    @NotEmpty
    private String groupName;
}

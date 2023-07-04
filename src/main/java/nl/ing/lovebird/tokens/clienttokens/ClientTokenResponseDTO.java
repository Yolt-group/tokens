package nl.ing.lovebird.tokens.clienttokens;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "ClientTokenResponse", description = "Object containing the Client Token and it's ttl")
class ClientTokenResponseDTO {

    @Schema(description = "The client token as JWT", required = true)
    private String clientToken;

    @Schema(description = "The lifetime in seconds of the client token", required = true)
    private long expiresIn;
}

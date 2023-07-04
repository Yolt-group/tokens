package nl.ing.lovebird.tokens.verificationkeys;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
public class VerificationKeyDTO {

    @Schema(description = "Client ID", required = true)
    UUID clientId;

    @Schema(description = "Key ID", required = true)
    String keyId;

    @Schema(description = "Verification key (public key)", required = true)
    String verificationKey;

    @Schema(description = "Time at which the key is created", required = true)
    Instant created;

    @Schema(description = "Time after which client should be notified that the keys will expire soon", required = true)
    Instant notifyTime;

    @Schema(description = "Time at which the key is expired", required = true)
    Instant expiryTime;
}

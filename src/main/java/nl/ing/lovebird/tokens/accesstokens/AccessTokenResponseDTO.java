package nl.ing.lovebird.tokens.accesstokens;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name = "AccessTokenResponse", description = "Object containing the API Access Token and its details")
class AccessTokenResponseDTO {

    @Schema(description = "The access token as JWT", required = true)
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "The access token type (e.g. 'Bearer')", required = true)
    @JsonProperty("token_type")
    private String tokenType;

    @Schema(description = "The lifetime in seconds of the access tokens", required = true)
    @JsonProperty("expires_in")
    private long expiresIn;

    @Schema(description = "Scopes, expressed as a list of space-delimited, case-sensitive strings")
    private String scope;

}

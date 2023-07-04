package nl.ing.lovebird.tokens.accesstokens;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.springdoc.annotations.ExternalApi;
import nl.ing.lovebird.tokens.exception.UnsupportedGrantTypeException;
import nl.ing.lovebird.tokens.util.Constants;
import org.jose4j.jwt.MalformedClaimException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "tokens")
class AccessTokenV1Controller {

    private static final String OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private final AccessTokenService accessTokenService;

    @Operation(summary = "Request an access token")
    @PostMapping(value = "/v1/tokens", consumes = APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8", produces = APPLICATION_JSON_VALUE)
    @ExternalApi
    public ResponseEntity<AccessTokenResponseDTO> getAccessToken(
            @Parameter(description = "Your JWT Request Token. Check docs to see how to generate it.")
            @RequestParam(value = "request_token")
            @Pattern(regexp = Constants.JWT_REGEX)
            @Size(max = Constants.MAX_SIZE_REQUEST_TOKEN)
                    String requestToken,
            @Parameter(description = "Grant type. Right now we only support 'client_credentials' value.")
            @RequestParam(value = "grant_type")
                    String grantType,
            HttpServletRequest request) throws MalformedClaimException {
        if (!OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS.equals(grantType)) {
            throw new UnsupportedGrantTypeException("unsupported grant_type: " + grantType);
        }

        AccessTokenResponse accessTokenResponse = accessTokenService.getAccessToken(requestToken, request);
        AccessTokenResponseDTO accessTokenResponseDTO = AccessTokenResponseDTOMapper.from(accessTokenResponse);
        return ResponseEntity.ok(accessTokenResponseDTO);
    }
}

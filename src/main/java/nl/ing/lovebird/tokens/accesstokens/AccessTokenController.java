package nl.ing.lovebird.tokens.accesstokens;

import lombok.RequiredArgsConstructor;
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
@Deprecated
class AccessTokenController {

    private static final String OAUTH2_GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private final AccessTokenService accessTokenService;

    /**
     * Clients should use {@link AccessTokenV1Controller#getAccessToken}
     */
    @Deprecated
    @PostMapping(value = "/tokens", consumes = APPLICATION_FORM_URLENCODED_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessTokenResponseDTO> getAccessTokenDeprecated(
            @RequestParam(value = "request_token")
            @Pattern(regexp = Constants.JWT_REGEX)
            @Size(max = Constants.MAX_SIZE_REQUEST_TOKEN)
                    String requestToken,
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

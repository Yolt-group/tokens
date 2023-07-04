package nl.ing.lovebird.tokens.clienttokens;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jose4j.jwt.MalformedClaimException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static nl.ing.lovebird.tokens.util.Constants.JWT_REGEX;
import static nl.ing.lovebird.tokens.util.Constants.MAX_SIZE_REQUEST_TOKEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "client-token")
public class ClientTokenController {

    private final ClientTokenRequestService clientTokenRequestService;

    @Operation(summary = "Request a client token which is used internally to authorize client access in service-service communication.")
    @PostMapping(value = "client-token", produces = APPLICATION_JSON_VALUE)
    public ClientTokenResponseDTO getClientToken(
            @Parameter(description = "Your JWT Request Token. Required claims: 'iss' should be registered in the tokens.client-token-requester.services-jwks and 'sub' should contain the client-id.")
            @RequestParam("request_token") @Size(max = MAX_SIZE_REQUEST_TOKEN) @Pattern(regexp = JWT_REGEX) String requestToken) throws MalformedClaimException {
        return clientTokenRequestService.requestClientToken(requestToken);
    }
}

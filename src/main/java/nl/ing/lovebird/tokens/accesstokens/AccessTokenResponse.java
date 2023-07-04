package nl.ing.lovebird.tokens.accesstokens;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Data
@RequiredArgsConstructor
class AccessTokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final List<Scope> scopes;

}

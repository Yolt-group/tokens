package nl.ing.lovebird.tokens.accesstokens;

import static java.util.stream.Collectors.joining;

class AccessTokenResponseDTOMapper {

    private AccessTokenResponseDTOMapper() {}

    public static AccessTokenResponseDTO from(AccessTokenResponse accessTokenResponse) {
        String scopes = "";

        if (accessTokenResponse.getScopes() != null) {
            scopes = accessTokenResponse.getScopes()
                    .stream()
                    .map(Scope::getCamelCaseName)
                    .collect(joining(" "));
        }

        return new AccessTokenResponseDTO(accessTokenResponse.getAccessToken(), accessTokenResponse.getTokenType(),
                accessTokenResponse.getExpiresIn(), scopes);

    }
}

package nl.ing.lovebird.tokens.authentication;

import org.jose4j.jwt.JwtClaims;

public interface RequestTokenAuthenticator {
    JwtClaims authenticate(String requestToken);
}

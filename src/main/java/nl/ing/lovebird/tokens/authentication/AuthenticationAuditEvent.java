package nl.ing.lovebird.tokens.authentication;

import lombok.Data;

import java.time.LocalDateTime;

@Data
class AuthenticationAuditEvent {

    private String requestToken;
    private LocalDateTime now = LocalDateTime.now();
    private String publicKey;

    public AuthenticationAuditEvent(String requestToken, String publicKey) {
        this.requestToken = requestToken;
        this.publicKey = publicKey;
    }
}

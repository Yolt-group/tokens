package nl.ing.lovebird.tokens.clienttokens;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonce;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonceRepository;
import nl.ing.lovebird.tokens.authentication.ServiceAuthenticationService;
import nl.ing.lovebird.tokens.exception.NonceAlreadyUsedException;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Services which have to do something for a client can request a client-token here.
 * Examples of services are client-gateway, api-gateway, site-management and dev-portal.
 * The client-token contains things like the client id and the client group id.
 */
@Service
@Slf4j
class ClientTokenRequestService {

    static final String GROUP_PREFIX = "group:";
    static final String CLIENT_PREFIX = "client:";
    static final String CLIENT_USER_PREFIX = "client-user:";

    private final ClientTokenService clientTokenService;
    private final ServiceAuthenticationService serviceAuthenticationService;
    private final RequestTokenNonceRepository requestTokenNonceRepository;
    private final long expirationTimeInSec;

    public ClientTokenRequestService(ClientTokenService clientTokenService, ServiceAuthenticationService serviceAuthenticationService, RequestTokenNonceRepository requestTokenNonceRepository, @Value("${service.tokens.client-token.expiration-time-in-sec}") long expirationTimeInSec) {
        this.clientTokenService = clientTokenService;
        this.serviceAuthenticationService = serviceAuthenticationService;
        this.requestTokenNonceRepository = requestTokenNonceRepository;
        this.expirationTimeInSec = expirationTimeInSec;
    }

    ClientTokenResponseDTO requestClientToken(String requestToken) throws MalformedClaimException {
        JwtClaims requestTokenClaims = serviceAuthenticationService.authenticate(requestToken);

        UUID nonceUuid = UUID.fromString(requestTokenClaims.getJwtId());
        checkNonce(nonceUuid);
        saveNonce(nonceUuid);

        String requester = requestTokenClaims.getIssuer();
        try {
            String clientToken;

            String subject = requestTokenClaims.getSubject();
            if (subject.startsWith(CLIENT_USER_PREFIX)) {
                String clientIdUserId = subject.substring(CLIENT_USER_PREFIX.length());
                UUID clientId = UUID.fromString(clientIdUserId.split(",")[0]);
                UUID userId = UUID.fromString(clientIdUserId.split(",")[1]);
                clientToken = clientTokenService.createClientUserToken(clientId, userId, expirationTimeInSec, requester);
            } else if (subject.startsWith(GROUP_PREFIX)) {
                UUID clientGroupId = UUID.fromString(subject.substring(GROUP_PREFIX.length()));
                clientToken = clientTokenService.createClientGroupToken(clientGroupId, expirationTimeInSec, requester);
            } else {
                UUID clientId = subject.startsWith(CLIENT_PREFIX) ? UUID.fromString(subject.substring(CLIENT_PREFIX.length())) : UUID.fromString(subject);
                clientToken = clientTokenService.createClientToken(clientId, expirationTimeInSec, requester);
            }
            log.info("Issued a client token to service: {} for {}", requester, subject); //NOSHERIFF
            return new ClientTokenResponseDTO(clientToken, expirationTimeInSec);
        } catch (JoseException e) {
            throw new IllegalStateException("Something failed during the creation of the client token", e);
        }
    }

    private void saveNonce(UUID nonceUuid) {
        requestTokenNonceRepository.save(new RequestTokenNonce(nonceUuid));
    }

    private void checkNonce(UUID nonceUuid) {
        requestTokenNonceRepository.getById(nonceUuid).ifPresent(requestTokenNonce -> {
            throw new NonceAlreadyUsedException();
        });
    }
}

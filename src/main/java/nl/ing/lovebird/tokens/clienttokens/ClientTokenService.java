package nl.ing.lovebird.tokens.clienttokens;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.logging.SemaEventLogger;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.tokens.authentication.sema.ClientRequestTokenInvalidSemaEvent;
import nl.ing.lovebird.tokens.clients.ClientService;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroup;
import nl.ing.lovebird.tokens.clients.dto.ClientDTO;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static nl.ing.lovebird.tokens.clienttokens.ClientTokenRequestService.*;

@Service
@Slf4j
public class ClientTokenService {

    private static final String CLAIM_CLIENT_GROUP_ID = "client-group-id";
    private static final String CLAIM_CLIENT_GROUP_NAME = "client-group-name";
    private static final String CLAIM_CLIENT_ID = "client-id";
    private static final String CLAIM_CLIENT_NAME = "client-name";
    private static final String CLAIM_CLIENT_USER_ID = "client-user-id";
    private static final String CLAIM_USER_ID = "user-id";
    private static final String CLAIM_ISSUED_FOR = "isf";
    private static final String CLAIM_CAM = "cam";
    private static final String CLAIM_CLIENT_USERS_KYC_PRIVATE_INDIVIDUALS = "client-users-kyc-private-individuals";
    private static final String CLAIM_CLIENT_USERS_KYC_ENTITIES = "client-users-kyc-entities";
    private static final String CLAIM_PSD2_LICENSED = "psd2-licensed";
    private static final String CLAIM_DATA_ENRICHMENT_MERCHANT_RECOGNITION = "data_enrichment_merchant_recognition";
    private static final String CLAIM_DATA_ENRICHMENT_CATEGORIZATION = "data_enrichment_categorization";
    private static final String CLAIM_DATA_ENRICHMENT_CYCLE_DETECTION = "data_enrichment_cycle_detection";
    private static final String CLAIM_DATA_ENRICHMENT_LABELS = "data_enrichment_labels";
    private static final String CLAIM_AIS = "ais";
    private static final String CLAIM_PIS = "pis";
    private static final String CLAIM_DELETED = "deleted";
    private static final String CLAIM_CONSENT_STARTER = "consent_starter";
    private static final String CLAIM_ONE_OFF_AIS = "one_off_ais";
    private static final String CLAIM_RISK_INSIGHTS = "risk_insights";
    private static final String JWT_SIGNING_VAULT_KEY_NAME = "tokens-jwk-secret";

    private final ClientUsersService clientUsersService;
    private final ClientService clientService;
    private final RsaJsonWebKey signatureJwk;

    public ClientTokenService(ClientUsersService clientUsersService, ClientService clientService, VaultKeys vaultKeys) {
        this.clientUsersService = clientUsersService;
        this.clientService = clientService;
        this.signatureJwk = vaultKeys.getRsaJsonWebKey(JWT_SIGNING_VAULT_KEY_NAME);
    }

    public String createClientUserToken(UUID clientId, UUID userId, long expirationTimeInSec, String requester) throws JoseException {
        var client = clientService.getClient(clientId);

        if (client.getGroup() == null) {
            throw new IllegalArgumentException("clientGroupId for client '" + clientId + "' with user '" + userId + "' is null");
        } else if (requester == null) {
            throw new IllegalArgumentException("Requester for client user token is null");
        } else if (requester.equals("consent-starter") && !client.getConsentStarter()) {
            throw createConsentStarterClaimException(clientId);
        }

        ClientUserDTO clientUser = clientUsersService.retrieveClientUserByUserId(clientId, userId);

        return createClientUserToken(client, clientUser, expirationTimeInSec, requester);
    }

    public String createClientToken(UUID clientId, long expirationTimeInSec, String requester) throws JoseException {
        var client = clientService.getClient(clientId);

        if (client.getGroup() == null) {
            throw new IllegalArgumentException("clientGroupId for client '" + clientId + "' is null");
        } else if (requester == null) {
            throw new IllegalArgumentException("Requester for client token is null");
        } else if (requester.equals("consent-starter") && !client.getConsentStarter()) {
            throw createConsentStarterClaimException(clientId);
        }

        return createClientToken(client, expirationTimeInSec, requester);
    }

    public String createClientGroupToken(UUID clientGroupId, long expirationTimeInSec, String requester) throws JoseException {
        if (StringUtils.isBlank(requester)) {
            throw new IllegalArgumentException("Requester for client group token is null or blank");
        }

        ClientGroup clientGroup = clientService.getClientGroup(clientGroupId);

        var claims = createJwtGroupClaims(clientGroupId, clientGroup, expirationTimeInSec, requester);
        return createClientToken(claims);
    }

    private String createClientUserToken(ClientDTO client, ClientUserDTO clientUser, long expirationTimeInSec, String forService) throws JoseException {
        var claims = createClientUserJwtClaims(client, clientUser, expirationTimeInSec, forService);
        return createClientToken(claims);
    }

    private String createClientToken(ClientDTO client, long expirationTimeInSec, String forService) throws JoseException {
        var claims = createClientJwtClaims(client, expirationTimeInSec, forService);
        return createClientToken(claims);
    }

    private String createClientToken(JwtClaims claims) throws JoseException {
        var jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA512);
        jws.setKey(signatureJwk.getPrivateKey());
        jws.setKeyIdHeaderValue(signatureJwk.getKeyId());
        return jws.getCompactSerialization();
    }

    private JwtClaims createClientUserJwtClaims(ClientDTO client, ClientUserDTO clientUser, long expirationTimeInSec, String forService) {
        var claims = createGenericJwtClaims(expirationTimeInSec, forService);
        claims.setSubject(CLIENT_USER_PREFIX + client.getClientId().toString() + "," + clientUser.getClientUserId());
        addClientClaims(client, claims);

        claims.setClaim(CLAIM_CLIENT_USER_ID, clientUser.getClientUserId());
        claims.setClaim(CLAIM_USER_ID, clientUser.getUserId());

        return claims;
    }

    private JwtClaims createClientJwtClaims(ClientDTO client, long expirationTimeInSec, String forService) {
        var claims = createGenericJwtClaims(expirationTimeInSec, forService);
        claims.setSubject(CLIENT_PREFIX + client.getClientId().toString());
        addClientClaims(client, claims);

        return claims;
    }

    private void addClientClaims(ClientDTO client, JwtClaims claims) {
        claims.setClaim(CLAIM_CLIENT_ID, client.getClientId().toString());
        claims.setClaim(CLAIM_CLIENT_NAME, client.getName());
        claims.setClaim(CLAIM_CLIENT_GROUP_ID, client.getGroup().getClientGroupId().toString());
        claims.setClaim(CLAIM_CLIENT_GROUP_NAME, client.getGroup().getGroupName());

        claims.setClaim(CLAIM_CAM, getWithFallback(client.getCam(), false));
        claims.setClaim(CLAIM_CLIENT_USERS_KYC_PRIVATE_INDIVIDUALS, getWithFallback(client.getClientUsersKycPrivateIndividuals(), false));
        claims.setClaim(CLAIM_CLIENT_USERS_KYC_ENTITIES, getWithFallback(client.getClientUsersKycEntities(), false));
        claims.setClaim(CLAIM_PSD2_LICENSED, getWithFallback(client.getPsd2Licensed(), true));
        claims.setClaim(CLAIM_DATA_ENRICHMENT_MERCHANT_RECOGNITION, getWithFallback(client.getDataEnrichmentMerchantRecognition(), false));
        claims.setClaim(CLAIM_DATA_ENRICHMENT_CATEGORIZATION, getWithFallback(client.getDataEnrichmentCategorization(), false));
        claims.setClaim(CLAIM_DATA_ENRICHMENT_CYCLE_DETECTION, getWithFallback(client.getDataEnrichmentCycleDetection(), false));
        claims.setClaim(CLAIM_DATA_ENRICHMENT_LABELS, getWithFallback(client.getDataEnrichmentLabels(), false));
        claims.setClaim(CLAIM_AIS, getWithFallback(client.getAis(), false));
        claims.setClaim(CLAIM_PIS, getWithFallback(client.getPis(), false));
        claims.setClaim(CLAIM_DELETED, getWithFallback(client.getDeleted(), false));
        claims.setClaim(CLAIM_CONSENT_STARTER, getWithFallback(client.getConsentStarter(), false));
        claims.setClaim(CLAIM_ONE_OFF_AIS, getWithFallback(client.getOneOffAis(), false));
        claims.setClaim(CLAIM_RISK_INSIGHTS, getWithFallback(client.getRiskInsights(), false));
    }

    private JwtClaims createJwtGroupClaims(UUID clientGroupId, ClientGroup clientGroup, long expirationTimeInSec, String forService) {
        var claims = createGenericJwtClaims(expirationTimeInSec, forService);
        claims.setSubject(GROUP_PREFIX + clientGroupId.toString());
        claims.setClaim(CLAIM_CLIENT_GROUP_ID, clientGroupId.toString());
        claims.setClaim(CLAIM_CLIENT_GROUP_NAME, clientGroup.getName());
        return claims;
    }

    private JwtClaims createGenericJwtClaims(long expirationTimeInSec, String forService) {
        var claims = new JwtClaims();
        claims.setIssuedAt(NumericDate.now());
        claims.setExpirationTime(determineExpirationDate(expirationTimeInSec));
        claims.setJwtId(UUID.randomUUID().toString());
        claims.setClaim(CLAIM_ISSUED_FOR, forService);
        return claims;
    }

    private boolean getWithFallback(Boolean value, boolean fallback) {
        return (value != null) ? value : fallback;
    }

    private NumericDate determineExpirationDate(long expirationTimeInSec) {
        var expirationDate = NumericDate.now();
        expirationDate.addSeconds(expirationTimeInSec);
        // Add 5 more seconds since we also communicate the expiration-in-seconds as part of the a response (oauth 2 spec).
        // The client should be able to rely on that, so the token actually expires a bit later.
        // For example, of expiration-in-seconds = 60. The token should still be valid for 60 seconds the moment the client
        // sees this response. Therefore, the actual token contains a value that is valid for a bit longer to adjust for
        // network latencies etc.
        expirationDate.addSeconds(5L);
        return expirationDate;
    }

    private IllegalArgumentException createConsentStarterClaimException(UUID clientId) {
        String msg = "The consent-starter is only allowed to request client-tokens for clients with consent-starter enabled. ClientId: " + clientId;
        SemaEventLogger.log(new ClientRequestTokenInvalidSemaEvent(msg, null, clientId));
        return new IllegalArgumentException(msg);
    }

}

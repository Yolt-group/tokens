package nl.ing.lovebird.tokens.authentication;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.logging.SemaEventLogger;
import nl.ing.lovebird.tokens.authentication.sema.ServiceRequestTokenInvalidSemaEvent;
import nl.ing.lovebird.tokens.authentication.sema.ServiceRequestTokenParsingFailedSemaEvent;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.VerificationJwkSelector;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * This service is responsible for authenticating internal services asking for client-tokens.
 * We verify them based on the specific JWKS that are registered under their issuer.
 */
@Slf4j
@Service
public class ServiceAuthenticationService implements RequestTokenAuthenticator {

    private static final AlgorithmConstraints JWS_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST,
            AlgorithmIdentifiers.RSA_PSS_USING_SHA512);

    private final Map<String, JsonWebKeySet> servicesAuthenticationJWKS;
    private final RequestTokenValidator requestTokenValidator;

    @Autowired
    public ServiceAuthenticationService(@Value("${request-token.validity-in-sec}") int validityTimeInSec,
                                        ClientTokenRequesterProperties clientTokenRequesterProperties) {
        requestTokenValidator = new RequestTokenValidator(validityTimeInSec, JWS_ALGORITHM_CONSTRAINTS);
        servicesAuthenticationJWKS = clientTokenRequesterProperties.getServicesJwks();

        servicesAuthenticationJWKS.forEach(
                (service, jwks) -> log.info("Read in {} verification keys for service: {}", jwks.getJsonWebKeys().size(), service));
    }

    public JwtClaims authenticate(String requestToken) {
        JsonWebSignature jsonWebSignature;
        JwtClaims unverifiedClaims;
        try {
            jsonWebSignature = (JsonWebSignature) JsonWebStructure.fromCompactSerialization(requestToken);
            unverifiedClaims = JwtClaims.parse(jsonWebSignature.getUnverifiedPayload());
        } catch (JoseException | InvalidJwtException e) {
            SemaEventLogger.log(new ServiceRequestTokenParsingFailedSemaEvent(
                    "Failed authenticating a service, could not parse the requestToken: " + requestToken));
            throw new IllegalArgumentException("Failed parsing the requestToken or the claims", e);
        }

        try {
            JsonWebKey verificationKey = getVerificationKey(jsonWebSignature, unverifiedClaims);
            return requestTokenValidator.validateRequestToken(requestToken, verificationKey.getKey());
        } catch (RuntimeException e) {
            String issuer = getIssuerFromClaims(unverifiedClaims);
            String keyId = jsonWebSignature.getKeyIdHeaderValue();
            log.error(String.format("Request token is invalid for issuer: '%s' and kid header: '%s'.", issuer, keyId), e); //NOSHERIFF - No vulnerable information, these are system-system tokens
            SemaEventLogger.log(new ServiceRequestTokenInvalidSemaEvent(issuer, keyId,
                    String.format("Request token is invalid for issuer: '%s' and kid header: '%s'. Request token: '%s'",
                    issuer, keyId, requestToken)));
            throw e;
        }
    }

    private JsonWebKey getVerificationKey(JsonWebSignature jsonWebSignature, JwtClaims unverifiedClaims) {
        String issuer = getIssuerFromClaims(unverifiedClaims);
        JsonWebKeySet issuerJWKS = servicesAuthenticationJWKS.get(issuer);
        VerificationJwkSelector jwkSelector = new VerificationJwkSelector();

        if (issuerJWKS == null) {
            throw new IllegalArgumentException("No public key set (jwks) found for service '" + issuer + "', cannot verify jws.");
        }

        try {
            JsonWebKey selectedKey = jwkSelector.select(jsonWebSignature, issuerJWKS.getJsonWebKeys());

            if (selectedKey == null) {
                throw new IllegalArgumentException("No valid verification key found for jws");
            }

            return selectedKey;
        } catch (JoseException e) {
            throw new IllegalArgumentException("Failed selecting the verification key for jws", e);
        }
    }

    private String getIssuerFromClaims(JwtClaims unverifiedClaims) {
        try {
            return unverifiedClaims.getIssuer();
        } catch (MalformedClaimException e) {
            log.warn("Could not retrieve issuer from claims of String type, this should not happen, since it's either null or a String...");
            return null;
        }
    }
}

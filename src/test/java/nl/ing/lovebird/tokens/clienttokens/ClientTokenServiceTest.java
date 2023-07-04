package nl.ing.lovebird.tokens.clienttokens;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.clients.cassandra.Client;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroup;
import nl.ing.lovebird.tokens.clients.ClientService;
import nl.ing.lovebird.tokens.clients.dto.ClientDTO;
import nl.ing.lovebird.tokens.clients.dto.ClientGroupDTO;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.lang.JoseException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.security.Security;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static nl.ing.lovebird.tokens.clienttokens.ClientTokenRequestService.GROUP_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class ClientTokenServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ACCESS_TOKEN_SIGNATURE_SECRET_JWK = "{\"kty\":\"RSA\",\"d\":\"bhWGPf59nIgmPL8kASoPsI145a5iWbfquBqsSvdR59lRrBjU1dObgpHEIX-AaEasV58V9jPqd0IBoCcaTyMm6XXaOySkoLkDKVXj_epeuEkPDGRG3UndCw7KybSBLIpd_zT4snlEgPLqt9kaZanABFAxYCS_DNHIlWwTPUxOsNSolk5fqAl7hTTdyb2rBWliyHQdY7z5cOQWtaq5QFh1dkiBrVMyRHDqaCS-4ABCUa19ckkirJLt6PqeuH3rWIV_zSIscZj6Uc_Egi9E91jDDdKDoVqvX8q-ZtQjWE3CZVyG4t75Bok3pgVAUzZqbWhxwMZnUT-ndvsIqupCVUKlwQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"9d257f29-dbbf-4fa3-a602-6eb0d498be0d\",\"alg\":\"RS256\",\"n\":\"qCICDKb5Llee82uF9jpmamRncej4tc8IrLYbmLDcTnvWv21iwLQh-KhUBTlYKfJ_KRonAFVXX2wxOGH_9zPztrfw7IhC8kavJrObjkE_aQkfvFeBJhpiLnxjwQr1cksdZdVCWascKSNN_eYe_emsZKjAcMUD-5D4OkF2bswhufxcp2AY1ghmB2gqYIIsrWIOMVV9oNOELmjSIna6zVFPTBUPH0I8PHb52C_-5zCyFGWtD7n2Pi8UUHME6Q20C1cqpYwThk9cnHrorR8E8-c8CoqKFEpLUmfVL0nBMBUUXi0HvYL2LTkUNtg3Skm2LLhktsPbpKpNBknrj4DrzR_R_Q\"}";
    private static final UUID CLIENT_ID_0 = UUID.randomUUID();
    private static final UUID CLIENT_GROUP_ID_0 = UUID.randomUUID();
    private static final String CLIENT_GROUP_NAME = "clientGroupName";

    @Mock
    private ClientUsersService clientUsersService;
    @Mock
    private ClientService clientService;
    @Mock
    private VaultKeys vaultKeys;

    private ClientTokenService clientTokenService;

    @Before
    public void before() throws JoseException {
        RsaJsonWebKey signatureJwk = (RsaJsonWebKey) RsaJsonWebKey.Factory.newPublicJwk(ACCESS_TOKEN_SIGNATURE_SECRET_JWK);
        when(vaultKeys.getRsaJsonWebKey("tokens-jwk-secret")).thenReturn(signatureJwk);
        clientTokenService = new ClientTokenService(clientUsersService, clientService, vaultKeys);
        ClientDTO client = new ClientDTO(new ClientGroupDTO(CLIENT_GROUP_ID_0, CLIENT_GROUP_NAME), CLIENT_ID_0);
        client.setName("clientName");
        when(clientService.getClient(CLIENT_ID_0)).thenReturn(client);
        when(clientService.getClientGroup(CLIENT_GROUP_ID_0)).thenReturn(new ClientGroup(CLIENT_GROUP_ID_0, CLIENT_GROUP_NAME));
    }

    @Test
    public void shouldSuccessFullyCreateClientToken() throws Exception {
        String requester = "site-management";
        String clientToken = clientTokenService.createClientToken(CLIENT_ID_0, 100, requester);

        // Verify JWS
        JwtConsumer jwsConsumer = TestUtil.getJwsConsumer(ACCESS_TOKEN_SIGNATURE_SECRET_JWK);
        JwtContext jwtContext = jwsConsumer.process(clientToken);

        JsonWebStructure innerJws = jwtContext.getJoseObjects().get(0);
        assertEquals("9d257f29-dbbf-4fa3-a602-6eb0d498be0d", innerJws.getKeyIdHeaderValue());
        Map<String, Object> claims = jwtContext.getJwtClaims().getClaimsMap();
        assertEquals("client:" + CLIENT_ID_0, claims.get("sub"));
        assertEquals(CLIENT_ID_0.toString(), claims.get("client-id"));
        assertEquals("clientName", claims.get("client-name"));
        assertEquals("clientGroupName", claims.get("client-group-name"));
        assertEquals(CLIENT_GROUP_ID_0.toString(), claims.get("client-group-id"));
        assertNotNull(claims.get("exp"));
        assertNotNull(claims.get("iat"));
        assertNotNull(claims.get("jti"));
        assertEquals(requester, claims.get("isf"));
        assertEquals(false, claims.get("client-users-kyc-private-individuals"));
        assertEquals(false, claims.get("client-users-kyc-entities"));
        assertEquals(true, claims.get("psd2-licensed"));
        assertEquals(false, claims.get("ais"));
        assertEquals(false, claims.get("pis"));
        assertEquals(false, claims.get("consent_starter"));
    }

    @Test
    public void shouldSuccessFullyCreateClientGroupToken() throws Exception {
        String requester = "site-management";
        String clientToken = clientTokenService.createClientGroupToken(CLIENT_GROUP_ID_0, 100, requester);

        // Verify JWS
        JwtConsumer jwsConsumer = TestUtil.getJwsConsumer(ACCESS_TOKEN_SIGNATURE_SECRET_JWK);
        JwtContext jwtContext = jwsConsumer.process(clientToken);

        JsonWebStructure innerJws = jwtContext.getJoseObjects().get(0);
        assertEquals("9d257f29-dbbf-4fa3-a602-6eb0d498be0d", innerJws.getKeyIdHeaderValue());
        Map<String, Object> claims = jwtContext.getJwtClaims().getClaimsMap();
        assertEquals(7, claims.size());
        assertEquals(GROUP_PREFIX + CLIENT_GROUP_ID_0, claims.get("sub"));
        assertEquals(CLIENT_GROUP_ID_0.toString(), claims.get("client-group-id"));
        assertEquals(CLIENT_GROUP_NAME, claims.get("client-group-name"));
        assertNotNull(claims.get("exp"));
        assertNotNull(claims.get("iat"));
        assertNotNull(claims.get("jti"));
        assertEquals(requester, claims.get("isf"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseOfNonRegisteredClientGroupId() throws Exception {
        when(clientService.getClient(CLIENT_ID_0)).thenReturn(new ClientDTO(null, CLIENT_ID_0));

        clientTokenService.createClientToken(CLIENT_ID_0, 123L, "api-gateway");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailBecauseOfNullClientTokenRequester() throws Exception {
        clientTokenService.createClientToken(CLIENT_ID_0, 123L, null);
    }

    @Test
    public void configurationWillBePutInTheToken() throws Exception {
        ClientDTO client = new ClientDTO(new ClientGroupDTO(CLIENT_GROUP_ID_0, "clientGroup"), CLIENT_ID_0);
        client.setName("client");
        client.setClientUsersKycPrivateIndividuals(true);
        client.setClientUsersKycEntities(true);
        client.setPsd2Licensed(false);
        client.setDataEnrichmentMerchantRecognition(true);
        client.setDataEnrichmentCategorization(false);
        client.setDataEnrichmentCycleDetection(true);
        client.setDataEnrichmentLabels(false);
        client.setAis(true);
        client.setPis(false);
        client.setConsentStarter(true);

        when(clientService.getClient(CLIENT_ID_0)).thenReturn(client);

        String clientToken = clientTokenService.createClientToken(CLIENT_ID_0, 100, "site-management");

        // Verify JWS
        JwtConsumer jwsConsumer = TestUtil.getJwsConsumer(ACCESS_TOKEN_SIGNATURE_SECRET_JWK);
        JwtContext jwtContext = jwsConsumer.process(clientToken);

        Map<String, Object> claims = jwtContext.getJwtClaims().getClaimsMap();
        assertEquals(true, claims.get("client-users-kyc-private-individuals"));
        assertEquals(true, claims.get("client-users-kyc-entities"));
        assertEquals(false, claims.get("psd2-licensed"));
        assertEquals(true, claims.get("data_enrichment_merchant_recognition"));
        assertEquals(false, claims.get("data_enrichment_categorization"));
        assertEquals(true, claims.get("data_enrichment_cycle_detection"));
        assertEquals(false, claims.get("data_enrichment_labels"));
        assertEquals(true, claims.get("ais"));
        assertEquals(false, claims.get("pis"));
        assertEquals(true, claims.get("consent_starter"));
    }

    @Test
    public void shouldFallbackToConfigurationDefaults() throws Exception {
        ClientDTO clientWithNullConfig = new ClientDTO(new ClientGroupDTO(CLIENT_GROUP_ID_0, "clientGroup"), CLIENT_ID_0);
        clientWithNullConfig.setName("client");
        when(clientService.getClient(CLIENT_ID_0)).thenReturn(clientWithNullConfig);

        String clientToken = clientTokenService.createClientToken(CLIENT_ID_0, 100, "site-management");

        // Verify JWS
        JwtConsumer jwsConsumer = TestUtil.getJwsConsumer(ACCESS_TOKEN_SIGNATURE_SECRET_JWK);
        JwtContext jwtContext = jwsConsumer.process(clientToken);

        Map<String, Object> claims = jwtContext.getJwtClaims().getClaimsMap();
        assertEquals(false, claims.get("client-users-kyc-private-individuals"));
        assertEquals(false, claims.get("client-users-kyc-entities"));
        assertEquals(true, claims.get("psd2-licensed"));
        assertEquals(false, claims.get("data_enrichment_merchant_recognition"));
        assertEquals(false, claims.get("data_enrichment_categorization"));
        assertEquals(false, claims.get("data_enrichment_cycle_detection"));
        assertEquals(false, claims.get("data_enrichment_labels"));
        assertEquals(false, claims.get("ais"));
        assertEquals(false, claims.get("pis"));
        assertEquals(false, claims.get("consent_starter"));
    }

    /**
     * Convenience method to create client tokens for separate environments.
     */
    @Ignore("Used for retrieving Client Tokens for manual testing")
    @Test
    public void createClientTokenForEnvironment() throws Exception {
        // client - definition
        ClientDTO defaultClient = new ClientDTO(new ClientGroupDTO(UUID.fromString("297ecda4-fd60-4999-8575-b25ad23b249c"), "Yolt"), UUID.fromString("0005291f-68bb-4d5f-9a3f-7aa330fb7641"), "Yolt", false, false, true, true, true, true, true, true, true, true, false, true, false, true); // Yolt.. use this on default namespace
        Client ycsClient = new Client(UUID.fromString("a2034b12-7dcc-11e8-adc0-fa7ae01bbebc"), "Cucumber Test", Collections.emptySet(), UUID.fromString("921ba0d6-f78f-43ec-845b-ee15338deb0a"));  // Yolt DevOps (use this on ycs)
        Client nonLicensedClient = new Client(UUID.fromString("7ac76b5f-ea3c-421e-8de9-8f93a14c5055"), "YTS Cucumber Test Non-Licensed Client", Collections.emptySet(), UUID.fromString("f767b2f9-5c90-4a4e-b728-9c9c8dadce4f"), true, true, true, false, true, true, false, false, false, false, false, true, true, true);  // Non licensed client under the YTS group (use this on ycs)
        Client ytsLicensedClient = new Client(UUID.fromString("8f8e3404-13ed-4aa7-b005-f9ad40f07234"), "YTS Cucumber Test Licensed Client", Collections.emptySet(), UUID.fromString("921ba0d6-f78f-43ec-845b-ee15338deb0a"), true, true, true, true, true, true, false, false, false, false, false, true, true, true);

        // --- Secret definitions, this is the private key which will sign the client-token
        // You can find these secrets in /vault/secrets/tokens-jwk-secret in a pod on each environment
        String teamsDefaultTokensSignatureSecretJwk = new String(Base64.decode("eyJrdHkiOiJSU0EiLCJkIjoiWXZLYWlZRnF5eVl0Um1JTEc1T3MzbUtUbEk0XzdTV0EtNVFTdFdUVkZBMlB5QmtxVFpTUW0xaWRFNmlCVHpzVHQ4VGNWaVhKRzJLWkZjcVF6RG9iNVFKNm84cjVUczhsSEFuX1oyY2EtczlET0UwTzNWa3dacmZJbWlhMnRsY1VtWGVob2dhWDEzUEhnZXppWkFPU2I0c0lXMExyZkFWcnJvTXRCUVFXSVEwVkNidEVSSWJDVXhmNDBxeHVXMjh1ZGZCQVJoNmh3WUl4clg0NDkwdVpHWHFrSmg3R0ZiT01id3hXMmMtS1pZV0U1akR4MzZuZUtiZnBfVXN0VkR2dUVwbkVLcm05R0NEZVJJU3JZX1BZVE5qSVczMGI0YnEzWGd3ZGZpMURvLXhzMlc5RWplTXlobzIzLUtoVWdzamoyMkNzSjRzQ3JqYWFNS2gxWlQ0LXdRIiwiZSI6IkFRQUIiLCJ1c2UiOiJzaWciLCJraWQiOiIyODA4ZjAzZC0xMDUwLTQ1ZjItOGMwMS05YzI0NDFlMDUwYTgiLCJhbGciOiJSUzUxMiIsIm4iOiJsUmFKNzM4YmJpTVVwbVRGTFZYaW9aZDd1OVQ5cW02TFNINUZkVzFBVXJKSkctRTRmbzZuXzMwX2Z0VUtjNFpacW90bEotU3Z1akh2cWZOVGJWa1Z5NG00ai15VmdUUU5QLU9Jc2dUUkVWY2tjT2swdjhiVGFRQUxCM3VDTEE3eGREcnIyNEszNXRTNUU2a0RRUThtOVJ2SGlRazRvd05MVDgxUGV2SGtvSDFsZmdVd09aSGZYTHpKQnFqX1BYUG9TSVQxeTlTZmtDbjdNMDBlYTI4bjJ4V2JHWFZfdmo4YmhKaEtTWEIyam5sSFhPMlFFWEg2djhWb0VObDlDYVd5amtKMHVKUFBDeWpSWU80LVAyRjkyWHc4OFJDMHJHR0gtQUFfc0k5bDh6T0h1Mk1LRkFMOXR4aTJpclVCcTliSWF5VFd2MHNqQmJ0SG1faG9weXdWYXcifQ=="), Charset.defaultCharset());
        String teamsYcsTokensSignatureSecretJwk = new String(Base64.decode("eyJrdHkiOiJSU0EiLCJkIjoiWXZLYWlZRnF5eVl0Um1JTEc1T3MzbUtUbEk0XzdTV0EtNVFTdFdUVkZBMlB5QmtxVFpTUW0xaWRFNmlCVHpzVHQ4VGNWaVhKRzJLWkZjcVF6RG9iNVFKNm84cjVUczhsSEFuX1oyY2EtczlET0UwTzNWa3dacmZJbWlhMnRsY1VtWGVob2dhWDEzUEhnZXppWkFPU2I0c0lXMExyZkFWcnJvTXRCUVFXSVEwVkNidEVSSWJDVXhmNDBxeHVXMjh1ZGZCQVJoNmh3WUl4clg0NDkwdVpHWHFrSmg3R0ZiT01id3hXMmMtS1pZV0U1akR4MzZuZUtiZnBfVXN0VkR2dUVwbkVLcm05R0NEZVJJU3JZX1BZVE5qSVczMGI0YnEzWGd3ZGZpMURvLXhzMlc5RWplTXlobzIzLUtoVWdzamoyMkNzSjRzQ3JqYWFNS2gxWlQ0LXdRIiwiZSI6IkFRQUIiLCJ1c2UiOiJzaWciLCJraWQiOiIyODA4ZjAzZC0xMDUwLTQ1ZjItOGMwMS05YzI0NDFlMDUwYTgiLCJhbGciOiJSUzUxMiIsIm4iOiJsUmFKNzM4YmJpTVVwbVRGTFZYaW9aZDd1OVQ5cW02TFNINUZkVzFBVXJKSkctRTRmbzZuXzMwX2Z0VUtjNFpacW90bEotU3Z1akh2cWZOVGJWa1Z5NG00ai15VmdUUU5QLU9Jc2dUUkVWY2tjT2swdjhiVGFRQUxCM3VDTEE3eGREcnIyNEszNXRTNUU2a0RRUThtOVJ2SGlRazRvd05MVDgxUGV2SGtvSDFsZmdVd09aSGZYTHpKQnFqX1BYUG9TSVQxeTlTZmtDbjdNMDBlYTI4bjJ4V2JHWFZfdmo4YmhKaEtTWEIyam5sSFhPMlFFWEg2djhWb0VObDlDYVd5amtKMHVKUFBDeWpSWU80LVAyRjkyWHc4OFJDMHJHR0gtQUFfc0k5bDh6T0h1Mk1LRkFMOXR4aTJpclVCcTliSWF5VFd2MHNqQmJ0SG1faG9weXdWYXcifQ=="), Charset.defaultCharset());
        String appAccTokensSignatureSecretJwk = new String(Base64.decode(""), Charset.defaultCharset());
        String yfbAccTokensSignatureSecretJwk = new String(Base64.decode(""), Charset.defaultCharset());

        // --- configure the right parameters for this token
        String requester = "dev-portal";
        String signatureSecretJwk = teamsDefaultTokensSignatureSecretJwk;
        ClientDTO client = defaultClient;
        int tenYearsOfExpirationTime = 60 * 60 * 24 * 365 * 10;

        // --- Mock the clientService
        ClientService clientService = mock(ClientService.class);
        when(clientService.getClient(client.getClientId())).thenReturn(client);

        RsaJsonWebKey signatureJwk = (RsaJsonWebKey) RsaJsonWebKey.Factory.newPublicJwk(signatureSecretJwk);
        reset(vaultKeys);
        when(vaultKeys.getRsaJsonWebKey("tokens-jwk-secret")).thenReturn(signatureJwk);
        ClientTokenService clientTokenService = new ClientTokenService(clientUsersService, clientService, vaultKeys);

        // Create the token and log it
        String token = clientTokenService.createClientToken(client.getClientId(), tenYearsOfExpirationTime, requester);
        log.info("Token: {}", token);
    }

    /**
     * Convenience method to create client group tokens for separate environments.
     */
    @Ignore("Used for retrieving Client Groups Tokens for manual testing")
    @Test
    public void createClientGroupTokenForEnvironment() throws Exception {
        // --- client group definitions
        ClientGroup defaultClientGroup = new ClientGroup(UUID.fromString("0005291f-68bb-4d5f-9a3f-7aa330fb7641"), "Yolt"); // Yolt.. use this on default namespace
        ClientGroup ycsClientGroup = new ClientGroup(UUID.fromString("921ba0d6-f78f-43ec-845b-ee15338deb0a"), "Yolt DevOps"); // Yolt DevOps (use this on ycs)
        ClientGroup nonLicensedClientGroup = new ClientGroup(UUID.fromString("f767b2f9-5c90-4a4e-b728-9c9c8dadce4f"), "Yolt DevOps");
        // --- Secret definitions, this is the private key which will sign the client-token
        // You can find these secrets in /vault/secrets/tokens-jwk-secret in a pod on each environment
        String teamsDefaultTokensSignatureSecretJwk = new String(Base64.decode("eyJrdHkiOiJSU0EiLCJkIjoiWXZLYWlZRnF5eVl0Um1JTEc1T3MzbUtUbEk0XzdTV0EtNVFTdFdUVkZBMlB5QmtxVFpTUW0xaWRFNmlCVHpzVHQ4VGNWaVhKRzJLWkZjcVF6RG9iNVFKNm84cjVUczhsSEFuX1oyY2EtczlET0UwTzNWa3dacmZJbWlhMnRsY1VtWGVob2dhWDEzUEhnZXppWkFPU2I0c0lXMExyZkFWcnJvTXRCUVFXSVEwVkNidEVSSWJDVXhmNDBxeHVXMjh1ZGZCQVJoNmh3WUl4clg0NDkwdVpHWHFrSmg3R0ZiT01id3hXMmMtS1pZV0U1akR4MzZuZUtiZnBfVXN0VkR2dUVwbkVLcm05R0NEZVJJU3JZX1BZVE5qSVczMGI0YnEzWGd3ZGZpMURvLXhzMlc5RWplTXlobzIzLUtoVWdzamoyMkNzSjRzQ3JqYWFNS2gxWlQ0LXdRIiwiZSI6IkFRQUIiLCJ1c2UiOiJzaWciLCJraWQiOiIyODA4ZjAzZC0xMDUwLTQ1ZjItOGMwMS05YzI0NDFlMDUwYTgiLCJhbGciOiJSUzUxMiIsIm4iOiJsUmFKNzM4YmJpTVVwbVRGTFZYaW9aZDd1OVQ5cW02TFNINUZkVzFBVXJKSkctRTRmbzZuXzMwX2Z0VUtjNFpacW90bEotU3Z1akh2cWZOVGJWa1Z5NG00ai15VmdUUU5QLU9Jc2dUUkVWY2tjT2swdjhiVGFRQUxCM3VDTEE3eGREcnIyNEszNXRTNUU2a0RRUThtOVJ2SGlRazRvd05MVDgxUGV2SGtvSDFsZmdVd09aSGZYTHpKQnFqX1BYUG9TSVQxeTlTZmtDbjdNMDBlYTI4bjJ4V2JHWFZfdmo4YmhKaEtTWEIyam5sSFhPMlFFWEg2djhWb0VObDlDYVd5amtKMHVKUFBDeWpSWU80LVAyRjkyWHc4OFJDMHJHR0gtQUFfc0k5bDh6T0h1Mk1LRkFMOXR4aTJpclVCcTliSWF5VFd2MHNqQmJ0SG1faG9weXdWYXcifQ=="), Charset.defaultCharset());
        String teamsYcsTokensSignatureSecretJwk = new String(Base64.decode("eyJrdHkiOiJSU0EiLCJkIjoiWXZLYWlZRnF5eVl0Um1JTEc1T3MzbUtUbEk0XzdTV0EtNVFTdFdUVkZBMlB5QmtxVFpTUW0xaWRFNmlCVHpzVHQ4VGNWaVhKRzJLWkZjcVF6RG9iNVFKNm84cjVUczhsSEFuX1oyY2EtczlET0UwTzNWa3dacmZJbWlhMnRsY1VtWGVob2dhWDEzUEhnZXppWkFPU2I0c0lXMExyZkFWcnJvTXRCUVFXSVEwVkNidEVSSWJDVXhmNDBxeHVXMjh1ZGZCQVJoNmh3WUl4clg0NDkwdVpHWHFrSmg3R0ZiT01id3hXMmMtS1pZV0U1akR4MzZuZUtiZnBfVXN0VkR2dUVwbkVLcm05R0NEZVJJU3JZX1BZVE5qSVczMGI0YnEzWGd3ZGZpMURvLXhzMlc5RWplTXlobzIzLUtoVWdzamoyMkNzSjRzQ3JqYWFNS2gxWlQ0LXdRIiwiZSI6IkFRQUIiLCJ1c2UiOiJzaWciLCJraWQiOiIyODA4ZjAzZC0xMDUwLTQ1ZjItOGMwMS05YzI0NDFlMDUwYTgiLCJhbGciOiJSUzUxMiIsIm4iOiJsUmFKNzM4YmJpTVVwbVRGTFZYaW9aZDd1OVQ5cW02TFNINUZkVzFBVXJKSkctRTRmbzZuXzMwX2Z0VUtjNFpacW90bEotU3Z1akh2cWZOVGJWa1Z5NG00ai15VmdUUU5QLU9Jc2dUUkVWY2tjT2swdjhiVGFRQUxCM3VDTEE3eGREcnIyNEszNXRTNUU2a0RRUThtOVJ2SGlRazRvd05MVDgxUGV2SGtvSDFsZmdVd09aSGZYTHpKQnFqX1BYUG9TSVQxeTlTZmtDbjdNMDBlYTI4bjJ4V2JHWFZfdmo4YmhKaEtTWEIyam5sSFhPMlFFWEg2djhWb0VObDlDYVd5amtKMHVKUFBDeWpSWU80LVAyRjkyWHc4OFJDMHJHR0gtQUFfc0k5bDh6T0h1Mk1LRkFMOXR4aTJpclVCcTliSWF5VFd2MHNqQmJ0SG1faG9weXdWYXcifQ=="), Charset.defaultCharset());
        String appAccTokensSignatureSecretJwk = new String(Base64.decode(""), Charset.defaultCharset());
        String yfbAccTokensSignatureSecretJwk = new String(Base64.decode(""), Charset.defaultCharset());

        // --- configure the right parameters for this token
        String requester = "dev-portal";
        String signatureSecretJwk = teamsDefaultTokensSignatureSecretJwk;
        ClientGroup clientGroup = defaultClientGroup;
        int tenYearsOfExpirationTime = 60 * 60 * 24 * 365 * 10;

        // --- Mock the clientService
        ClientService clientService = mock(ClientService.class);
        when(clientService.getClientGroup(clientGroup.getId())).thenReturn(clientGroup);

        RsaJsonWebKey signatureJwk = (RsaJsonWebKey) RsaJsonWebKey.Factory.newPublicJwk(signatureSecretJwk);
        reset(vaultKeys);
        when(vaultKeys.getRsaJsonWebKey("tokens-jwk-secret")).thenReturn(signatureJwk);
        ClientTokenService clientTokenService = new ClientTokenService(clientUsersService, clientService, vaultKeys);

        // Create the token and log it
        String clientGroupToken = clientTokenService.createClientGroupToken(clientGroup.getId(), tenYearsOfExpirationTime, requester);
        log.info("Token: {}", clientGroupToken);
    }
}

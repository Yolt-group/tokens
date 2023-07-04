package nl.ing.lovebird.tokens.clienttokens;

import nl.ing.lovebird.tokens.IntegrationTest;
import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.clients.cassandra.Client;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroup;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroupRepository;
import nl.ing.lovebird.tokens.clients.cassandra.ClientRepository;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@IntegrationTest
public class ClientTokenIT {

    // The private key below is paired with the public key in tokens.client-token-requester.services-jwks
    private static final String JWK_ID = "d91706d8-dbc5-4e5c-a98c-018f1dbbe3b3";
    private static final String CLIENT_TOKEN_REQUESTER_PRIVATE_KEY = String.format("{\"use\":\"sig\",\"kty\":\"RSA\",\"kid\":\"%s\",\"alg\":\"PS512\",\"n\":\"3gIoBnouy6u2hhuwHmZ1xNqWYj21V8cjSEcojriZTmQjFlDPRKYi90ohXQImarPtSwZZ43VyGWfPzDBYsjv1kpoxi_fIO2TUo7Hya9ZBlKfUPS02cuUg8_vvuPJOKjtyYcxT-EiAhuVCHQO5xPW0txNfEmcZabM--U2f0xDkYkvSH0UdLITOuGxJ2Z3c9dnUktYKtuFgLJ6IUSbKEVu3c4gVlKKIZqUwaqTeDN_Ta2qo42eAGB-NDkV2Fb-lWv0pAP4JrmzzR4T4CQjxC64JnMj5WS-HLVB3ZYGeyd_TdMOl54E1bqceLUXd3lLS-H3uA20qFwJIiSN2mKJfsu1zyQ\",\"e\":\"AQAB\",\"d\":\"OVuNQ78lf_FpQtS8d9445xcia1dOFtOYRgA8dkMzOJGejxFpu27A8erxG7qGemLqF8wYRS_-vpmXkkWbZNEg7TNxQZxO2yl91p0O6NV-NA34Q6X-v0h7bw3ULZBMqZSaRLIpr6frrO5mWmCwCjcA7ZvPoE2Kv0hUKMzpUfq3Rr2cyREQ7tKceLdd87-utPI6qLl0AnNgzyv_XWyALaAhFFBMLO6VLUClIsAxuZ3pEtz6rnKNzdNwRFvDmt2h-Szfz6H9DCoRwdrIZccrRmQlUbdnQujdQ99P9g3UCwMY5Ev32bTjNm0uQwmjCR8NIB72FY7RsKmkPvP0eaRf8NwRQQ\",\"p\":\"-T201bS3OlaRRSl-YJPpffNWMTiRaf_wgA7iqw1zh-fnzI7pAea6lzFwR5arRM0UAmtPcyYYc1TDlnIpT7-ImMYozuliA3OsEmfgZDYl1zZQObtpyfLJxVyeDgtgt6HlMNqh4jWUF7fO7sG7Dni962Rq9rCdU3eORiN9mpSy1GU\",\"q\":\"5Adk8n_K_WF-wA9KFc3Baf3Xl0W3BqVwyn15rgL-w2fNg8mhD4sjOsRnYaNELhXPddqnMkENeruvHHB3HZAaYIFxbpDsuspIn_OI71PwKVxr6ZzsgJ6L9aCo3u2uhjZXSdggBZShf8XXHzW4RTT_uElrcUanaHNAG0Q0aN8msZU\",\"dp\":\"NPoCt_HjiEjiM9sv-4M0_Ap-5ZNqhSdbjTycvVLsnZSTVo2BxV-vhXeCsBS8brFQcXsxtf4A8HKx6AZ39XV2qVJlViWRKb6qcncV4PcJcAchYkQNplkuvf6DseSFOEL3Hj06b4SH58qv91vCUL8lTyVW_vxnVFhzK0Fb70FwCWk\",\"dq\":\"KYJXE_XDY-CNjDS8Tb_Ix5yX1Eeyi5kuMQ4qSkztR3Dr7mOT2BqB7T63ewSXcrQxsch9yx4gcAkeVVT7ufvzcUHe_juPfGk6UzOn57kl7MGQO1R_kOJLpAj89Kfbrz7RIPYgziNCStoe0OWch6ygKFHlJxevfcoND2mZCMYmawk\",\"qi\":\"E-n856UD2hKUNTVZC2WKSZjz19w-wu-7NmKvhmOhgRYUndXMB_qVtg9dMko-Tde2zvlNQRs-UXvu2iRm43NfHtsTOfG_crzrZp8FpzFQPv3V832tYb10wBWxS4Dd0oout1g5sU7e8rfNRMCsZngRPlPfEwp4MwFPKzTvoIyZZ0M\"}", JWK_ID);
    private static final String JWT_SIGNING_KEY = "{\"kty\":\"RSA\",\"d\":\"bhWGPf59nIgmPL8kASoPsI145a5iWbfquBqsSvdR59lRrBjU1dObgpHEIX-AaEasV58V9jPqd0IBoCcaTyMm6XXaOySkoLkDKVXj_epeuEkPDGRG3UndCw7KybSBLIpd_zT4snlEgPLqt9kaZanABFAxYCS_DNHIlWwTPUxOsNSolk5fqAl7hTTdyb2rBWliyHQdY7z5cOQWtaq5QFh1dkiBrVMyRHDqaCS-4ABCUa19ckkirJLt6PqeuH3rWIV_zSIscZj6Uc_Egi9E91jDDdKDoVqvX8q-ZtQjWE3CZVyG4t75Bok3pgVAUzZqbWhxwMZnUT-ndvsIqupCVUKlwQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"9d257f29-dbbf-4fa3-a602-6eb0d498be0d\",\"alg\":\"RS256\",\"n\":\"qCICDKb5Llee82uF9jpmamRncej4tc8IrLYbmLDcTnvWv21iwLQh-KhUBTlYKfJ_KRonAFVXX2wxOGH_9zPztrfw7IhC8kavJrObjkE_aQkfvFeBJhpiLnxjwQr1cksdZdVCWascKSNN_eYe_emsZKjAcMUD-5D4OkF2bswhufxcp2AY1ghmB2gqYIIsrWIOMVV9oNOELmjSIna6zVFPTBUPH0I8PHb52C_-5zCyFGWtD7n2Pi8UUHME6Q20C1cqpYwThk9cnHrorR8E8-c8CoqKFEpLUmfVL0nBMBUUXi0HvYL2LTkUNtg3Skm2LLhktsPbpKpNBknrj4DrzR_R_Q\"}";

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientGroupRepository clientGroupRepository;

    @Autowired
    private TestRestTemplate restTemplate;


    @Test
    public void testGetClientToken() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID clientGroupId = UUID.randomUUID();
        clientRepository.save(new Client(clientId, "Name", Collections.emptySet(), clientGroupId));
        clientGroupRepository.save(new ClientGroup(clientGroupId, "name"));
        String requestToken = createRequestToken("dev-portal", clientId, UUID.randomUUID().toString());

        ClientTokenResponseDTO clientTokenResponse = restTemplate.postForObject("/client-token?request_token={token}",
                Void.class,
                ClientTokenResponseDTO.class,
                requestToken);

        assertNotNull(clientTokenResponse);
        assertEquals(7200, clientTokenResponse.getExpiresIn());
        assertNotNull(clientTokenResponse.getClientToken());

        RsaJsonWebKey rsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(JWT_SIGNING_KEY);
        JwtConsumer jwsConsumer = TestUtil.getJwsConsumer(rsaJsonWebKey);
        JwtClaims claims = jwsConsumer.process(clientTokenResponse.getClientToken()).getJwtClaims();

        assertEquals("client:" + clientId.toString(), claims.getSubject());
        assertEquals(clientId.toString(), claims.getStringClaimValue("client-id"));
        assertEquals(clientGroupId.toString(), claims.getStringClaimValue("client-group-id"));
        assertNotNull(claims.getClaimValue("exp"));
        assertNotNull(claims.getClaimValue("iat"));
        assertNotNull(claims.getClaimValue("jti"));
        assertEquals("dev-portal", claims.getStringClaimValue("isf"));
        assertEquals(false, claims.getClaimValue("client-users-kyc-private-individuals", Boolean.class));
        assertEquals(false, claims.getClaimValue("client-users-kyc-entities", Boolean.class));
        assertEquals(true, claims.getClaimValue("psd2-licensed", Boolean.class));
    }

    private String createRequestToken(String requester, UUID clientId, String jwtId) throws Exception {
        RsaJsonWebKey rsaJsonWebKey = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(CLIENT_TOKEN_REQUESTER_PRIVATE_KEY);

        JsonWebSignature requestToken = new JsonWebSignature();
        requestToken.setPayload(createClaims(jwtId, requester, clientId).toJson());
        requestToken.setKey(rsaJsonWebKey.getRsaPrivateKey());
        requestToken.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_PSS_USING_SHA512);
        requestToken.setKeyIdHeaderValue(JWK_ID);

        return requestToken.getCompactSerialization();
    }

    private JwtClaims createClaims(String jwtId, String requester, UUID clientId) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(requester);
        claims.setSubject(clientId.toString());
        claims.setJwtId(jwtId);
        claims.setIssuedAt(NumericDate.now());
        claims.setExpirationTime(NumericDate.fromSeconds(15 + NumericDate.now().getValue()));

        return claims;
    }
}

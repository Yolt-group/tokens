package nl.ing.lovebird.tokens.accesstokens;

import nl.ing.lovebird.tokens.IntegrationTest;
import nl.ing.lovebird.tokens.clients.cassandra.Client;
import nl.ing.lovebird.tokens.clients.cassandra.ClientRepository;
import nl.ing.lovebird.tokens.configuration.ErrorConstants;
import nl.ing.lovebird.tokens.verificationkeys.VerificationKeyRepository;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.tokens.TestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@IntegrationTest
public class AccessTokenIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private VerificationKeyRepository verificationKeyRepository;

    @Test
    public void testGetAccessToken() throws Exception {
        // 0. Preparing data
        UUID clientId = UUID.randomUUID();
        UUID clientGroupId = UUID.randomUUID();
        clientRepository.save(new Client(clientId, "test-client", Collections.emptySet(), clientGroupId));
        String keyId = UUID.randomUUID().toString();
        JsonWebSignature requestToken = createValidRequestToken(clientId, getPrivateKey(), keyId);

        // 1. Get error when trying to obtain access token against no verification keys
        ResponseEntity<String> errorTokenResponse = restTemplate.postForEntity(new URI("/tokens"),
                new HttpEntity<>(body(requestToken), headers(MediaType.APPLICATION_FORM_URLENCODED, true)),
                String.class);
        assertThat(errorTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(errorTokenResponse.getBody()).contains("T" + ErrorConstants.NO_VERIFICATION_KEY.getCode());

        // 2. Save verification key
        verificationKeyRepository.putVerificationKey(clientId, keyId, getPublicKeyFromPEM(), Instant.now());

        // 3. Successfully get access token
        ResponseEntity<AccessTokenResponseDTO> successTokenResponse = restTemplate.postForEntity(new URI("/tokens"),
                new HttpEntity<>(body(requestToken), headers(MediaType.APPLICATION_FORM_URLENCODED, true)),
                AccessTokenResponseDTO.class);
        assertThat(successTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(successTokenResponse.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(successTokenResponse.getBody().getAccessToken()).isNotBlank();
    }

    private static String body(JsonWebSignature requestToken) throws IOException, JoseException {
        List<NameValuePair> requestParams = Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("request_token", requestToken.getCompactSerialization())
        );
        return IOUtils.toString(new UrlEncodedFormEntity(requestParams).getContent(), Charset.defaultCharset());
    }

    private static HttpHeaders headers(MediaType contentType, boolean withIp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        if (withIp) {
            headers.set("Forwarded", "for=192.168.1.1; for=192.168.1.100");
        }
        return headers;
    }
}

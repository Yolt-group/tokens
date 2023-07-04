package nl.ing.lovebird.tokens.clientcertificates;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(ClientCertificatesController.class)
public class ClientCertificatesControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestClientTokens testClientTokens;
    @MockBean
    private ClientCertificatesService service;

    @Test
    public void listVerificationKeys() throws Exception {
        UUID clientId = UUID.randomUUID();
        mockMvc.perform(get("/clients/" + clientId + "/client-certificates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
        verify(service).getClientCertificates(clientId);
    }

    @Test
    public void requestClientCertificate() throws Exception {
        UUID clientId = UUID.randomUUID();
        String content = "-----BEGIN CERTIFICATE REQUEST-----\n" +
                "MIICszCCAZsCAQAwbjELMAkGA1UEBhMCTkwxCzAJBgNVBAgMAk5IMQ8wDQYDVQQH\n" +
                "DAZBbmRpamsxGjAYBgNVBAoMEVRlYW0gUm9ja3N0YXJzIElUMSUwIwYDVQQDDBxy\n" +
                "YWxwaC5ydWlqc0B0ZWFtcm9ja3N0YXJzLm5sMIIBIjANBgkqhkiG9w0BAQEFAAOC\n" +
                "AQ8AMIIBCgKCAQEApci1qq+oLJzx5RCBWcdQ+K6BHPT7RgZDq7GctXxkUdb0R4Jf\n" +
                "8FaxkqWxTgvh284uiZTQ2QH0nnfJp7asEsk3oU8k3SM/oBDAaTcjJ/gxGSuAKm4T\n" +
                "UjidVbHPMEk3K5q9caL5jVvoGF8vjF1SppR+7sBmLaU310rglITRpP6+/BwH2g7R\n" +
                "jHE552KDw7lOa2CmoDKJsAvraBbGFKpOXcm30K4r+4lQNg+udlF+YaYgLRlBb1CR\n" +
                "pUeAHap1MXMaehxrwM2/NZ9LvCV+Xc5nlS0K6XdxyfT4aAtlrH1iz+MDsShJLSlf\n" +
                "lj/oPVc64BfJE7fAAiTROz36lNVRE+X/W1yhWQIDAQABoAAwDQYJKoZIhvcNAQEL\n" +
                "BQADggEBAE2G1m21RlPpUlcDN6/g81GvH64288WpgXHiImnEU/xYns6dnQOwt7zs\n" +
                "/2eiVeG7vXAeB4WWC8z11oX8KKiJ4zSBi0gsaVA2XQH9ezvafZhBXzdx6p4a6ZJw\n" +
                "DL/UOoHQUq5OH/VxV34Nx802rLA62+LBiTF1fnNYDPQ1t49Xeu+eUx+u2jSmkhWV\n" +
                "kZnkbCWM7WlRQQJEAf7qqI0cfzph3rQLre9bgvvKgHyEr0tcqgtlOGCcaM9tfuyC\n" +
                "6Ujq+WDbjpsl8MpV7o8EEagURT6X1p9PdZbR9h+3jZqFzQ0qCBKkbCDS6BGLmMCG\n" +
                "+6+4llZXRR9Wh12d/LAEQaolqN83Ypo=\n" +
                "-----END CERTIFICATE REQUEST-----";

        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), clientId);

        mockMvc.perform(post("/clients/" + clientId + "/client-certificates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                        .content(content))
                .andExpect(status().isOk());
        verify(service).requestClientProxyClientCertificate(clientToken, content);
    }
}

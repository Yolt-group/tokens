package nl.ing.lovebird.tokens.accesstokens;

import nl.ing.lovebird.tokens.configuration.ErrorConstants;
import nl.ing.lovebird.tokens.util.Constants;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(AccessTokenController.class)
public class AccessTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccessTokenService accessTokenService;

    @Test
    public void shouldSuccessfullyGetAccessToken() throws Exception {
        when(accessTokenService.getAccessToken(anyString(), any(HttpServletRequest.class)))
                .thenReturn(new AccessTokenResponse("someAccessToken", "Bearer", 600, null));
        final String requestToken = "aaa.aaa.aaa";

        final String requestContent = EntityUtils.toString(new UrlEncodedFormEntity(asList(
                new BasicNameValuePair("request_token", requestToken),
                new BasicNameValuePair("grant_type", "client_credentials"))));

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/tokens")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED);

        mockMvc.perform(request)
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.access_token", is("someAccessToken")))
                .andExpect(jsonPath("$.token_type", is("Bearer")))
                .andExpect(jsonPath("$.expires_in", is(600)))
                .andExpect(jsonPath("$.scope", is("")));

        verify(accessTokenService).getAccessToken(eq(requestToken), any(HttpServletRequest.class));
    }

    @Test
    public void shouldFailForUnsupportedGrantTypes() throws Exception {
        final String requestContent = EntityUtils.toString(new UrlEncodedFormEntity(asList(
                new BasicNameValuePair("request_token", "aaa.aaa.aaa"),
                new BasicNameValuePair("grant_type", "authorization_code"))));

        MockHttpServletRequestBuilder request =
                MockMvcRequestBuilders.post("/tokens")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED);


        mockMvc.perform(request)
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.code", is("T004")))
                .andExpect(jsonPath("$.message", is("Invalid grant type")));

        verify(accessTokenService, never()).getAccessToken(anyString(), any(HttpServletRequest.class));
    }

    @Test
    public void shouldFailForUnsupportedContentTypes() throws Exception {
        final String requestContent = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                " <!DOCTYPE foo [  \n" +
                "  <!ELEMENT foo ANY >\n" +
                "  <!ENTITY xxe SYSTEM \"file:///dev/random\" >]><foo>&xxe;</foo>";

        mockMvc.perform(MockMvcRequestBuilders.post("/tokens", requestContent, MediaType.APPLICATION_XML))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.code", is("T013")))
                .andExpect(jsonPath("$.message", is("Unsupported Content-Type")));

        verify(accessTokenService, never()).getAccessToken(anyString(), any(HttpServletRequest.class));
    }

    @Test
    public void testJwtPattern() {
        Pattern pattern = Pattern.compile(Constants.JWT_REGEX);
        Assert.assertTrue(pattern.matcher("abcdefghij.abcdefghij.abcdefghij").matches());
        Assert.assertFalse(pattern.matcher("abcdefghij.abcdefghij.abcdefghij=").matches());
        Assert.assertFalse(pattern.matcher("abcdefghij.abcdefghij.abcdefghij==").matches());
        Assert.assertTrue("Special chars", pattern.matcher("abcdefghij.abcdefghij.abcdefghij-_").matches());
        Assert.assertTrue("Empty part", pattern.matcher("abcdefghij..abcdefghij").matches());
        Assert.assertFalse("Invalid char", pattern.matcher("abcdefghij.abcdefghij.abcdefghij$").matches());
        Assert.assertFalse("Two parts", pattern.matcher("abcdefghij.abcdefghij").matches());
        Assert.assertFalse("Four parts", pattern.matcher("abcdefghij.abcdefghij.abcdefghij.abcdefghij").matches());
    }

    @Test
    public void whenInvalidToken_shouldReturnBadRequest() throws Exception {
        RequestBuilder requestBuilder = requestBuilder("abcdefghi");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.code", is("T" + ErrorConstants.INVALID_REQUEST_PARAMETERS.getCode())))
                .andExpect(jsonPath("$.message", is(ErrorConstants.INVALID_REQUEST_PARAMETERS.getMessage())));
        verify(accessTokenService, never()).getAccessToken(anyString(), any(HttpServletRequest.class));
    }

    @Test
    public void whenTokenValid_shouldReturn() throws Exception {
        String validToken = "abcdefghij.abcdefghij.abcdefghij";
        AccessTokenResponse tokenResponse = new AccessTokenResponse(validToken, "type", new Date().getTime(), null);
        when(accessTokenService.getAccessToken(anyString(), any(HttpServletRequest.class))).thenReturn(tokenResponse);

        RequestBuilder requestBuilder = requestBuilder(validToken);

        mockMvc.perform(requestBuilder)
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.access_token", is(validToken)))
                .andExpect(jsonPath("$.token_type", is(tokenResponse.getTokenType())))
                .andExpect(jsonPath("$.expires_in", is(tokenResponse.getExpiresIn())))
                .andExpect(jsonPath("$.scope", is("")));
    }

    private static RequestBuilder requestBuilder(String value) throws IOException {
        List<NameValuePair> requestParams = Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("request_token", value)
        );
        String requestContent = EntityUtils.toString(new UrlEncodedFormEntity(requestParams));
        return post("/tokens").content(requestContent).contentType(MediaType.APPLICATION_FORM_URLENCODED);
    }
}

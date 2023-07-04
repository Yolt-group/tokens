package nl.ing.lovebird.tokens;

import com.datastax.driver.core.ConsistencyLevel;
import com.yolt.securityutils.crypto.PasswordKey;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.tokens.authentication.RequestTokenNonceRepository;
import org.bouncycastle.util.encoders.Base64;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.PostConstruct;

@Configuration
@ComponentScan("nl.ing.lovebird.testsupport")
public class TestConfiguration {

    @Autowired
    private VaultKeys vaultKeys;

    @Autowired
    private RequestTokenNonceRepository requestTokenNonceRepository;

    @PostConstruct
    public void onInit() throws JoseException {
        ReflectionTestUtils.setField(requestTokenNonceRepository, "writeConsistency", ConsistencyLevel.ONE);

        vaultKeys.addPrivate("cgw-tokens-encryption", new PasswordKey(new String(Base64.decode("RmRoOXU4cklOeGZpdmJyaWFuYmJWVDF1MjMyVlFCWllLeDFIR0FHUHQySUZkaDl1OHJJTnhmaXZicmlhbmJiVlQxdTIzMlZRQlpZS3gxSEdBR1B0MkkK")).toCharArray()));

        String publicJWKJson = new String(Base64.decode("eyJrdHkiOiJSU0EiLCJkIjoiYmhXR1BmNTluSWdtUEw4a0FTb1BzSTE0NWE1aVdiZnF1QnFzU3ZkUjU5bFJyQmpVMWRPYmdwSEVJWC1BYUVhc1Y1OFY5alBxZDBJQm9DY2FUeU1tNlhYYU95U2tvTGtES1ZYal9lcGV1RWtQREdSRzNVbmRDdzdLeWJTQkxJcGRfelQ0c25sRWdQTHF0OWthWmFuQUJGQXhZQ1NfRE5ISWxXd1RQVXhPc05Tb2xrNWZxQWw3aFRUZHliMnJCV2xpeUhRZFk3ejVjT1FXdGFxNVFGaDFka2lCclZNeVJIRHFhQ1MtNEFCQ1VhMTlja2tpckpMdDZQcWV1SDNyV0lWX3pTSXNjWmo2VWNfRWdpOUU5MWpERGRLRG9WcXZYOHEtWnRRaldFM0NaVnlHNHQ3NUJvazNwZ1ZBVXpacWJXaHh3TVpuVVQtbmR2c0lxdXBDVlVLbHdRIiwiZSI6IkFRQUIiLCJ1c2UiOiJzaWciLCJraWQiOiI5ZDI1N2YyOS1kYmJmLTRmYTMtYTYwMi02ZWIwZDQ5OGJlMGQiLCJhbGciOiJSUzI1NiIsIm4iOiJxQ0lDREtiNUxsZWU4MnVGOWpwbWFtUm5jZWo0dGM4SXJMWWJtTERjVG52V3YyMWl3TFFoLUtoVUJUbFlLZkpfS1JvbkFGVlhYMnd4T0dIXzl6UHp0cmZ3N0loQzhrYXZKck9iamtFX2FRa2Z2RmVCSmhwaUxueGp3UXIxY2tzZFpkVkNXYXNjS1NOTl9lWWVfZW1zWktqQWNNVUQtNUQ0T2tGMmJzd2h1ZnhjcDJBWTFnaG1CMmdxWUlJc3JXSU9NVlY5b05PRUxtalNJbmE2elZGUFRCVVBIMEk4UEhiNTJDXy01ekN5RkdXdEQ3bjJQaThVVUhNRTZRMjBDMWNxcFl3VGhrOWNuSHJvclI4RTgtYzhDb3FLRkVwTFVtZlZMMG5CTUJVVVhpMEh2WUwyTFRrVU50ZzNTa20yTExoa3RzUGJwS3BOQmtucmo0RHJ6Ul9SX1EifQ=="));
        JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(publicJWKJson);
        vaultKeys.addPrivate("tokens-jwk-secret", jsonWebKey);

        publicJWKJson = new String(Base64.decode("eyJrdHkiOiJSU0EiLCJlIjoiQVFBQiIsInVzZSI6InNpZyIsImtpZCI6IjlkMjU3ZjI5LWRiYmYtNGZhMy1hNjAyLTZlYjBkNDk4YmUwZCIsImFsZyI6IlJTMjU2IiwibiI6InFDSUNES2I1TGxlZTgydUY5anBtYW1SbmNlajR0YzhJckxZYm1MRGNUbnZXdjIxaXdMUWgtS2hVQlRsWUtmSl9LUm9uQUZWWFgyd3hPR0hfOXpQenRyZnc3SWhDOGthdkpyT2Jqa0VfYVFrZnZGZUJKaHBpTG54andRcjFja3NkWmRWQ1dhc2NLU05OX2VZZV9lbXNaS2pBY01VRC01RDRPa0YyYnN3aHVmeGNwMkFZMWdobUIyZ3FZSUlzcldJT01WVjlvTk9FTG1qU0luYTZ6VkZQVEJVUEgwSThQSGI1MkNfLTV6Q3lGR1d0RDduMlBpOFVVSE1FNlEyMEMxY3FwWXdUaGs5Y25Icm9yUjhFOC1jOENvcUtGRXBMVW1mVkwwbkJNQlVVWGkwSHZZTDJMVGtVTnRnM1NrbTJMTGhrdHNQYnBLcE5Ca25yajREcnpSX1JfUSJ9"));
        PublicJsonWebKey publicJsonWebKey = org.jose4j.jwk.PublicJsonWebKey.Factory.newPublicJwk(publicJWKJson);
        vaultKeys.addPublic("tokens-jwk-secret", publicJsonWebKey);
    }

}

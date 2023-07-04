package nl.ing.lovebird.tokens.authentication;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jose4j.jwk.JsonWebKeySet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "tokens.client-token-requester")
@Data
@AllArgsConstructor
public class ClientTokenRequesterProperties {
    private Map<String, JsonWebKeySet> servicesJwks;
}

package nl.ing.lovebird.tokens.clienttokens;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class ClientUsersService {

    private final RestTemplate restTemplate;

    public ClientUsersService(RestTemplateBuilder restTemplateBuilder,
                              @Value("${service.users.url}") String usersUrl) {
        this.restTemplate = restTemplateBuilder.rootUri(usersUrl).build();
    }

    public ClientUserDTO retrieveClientUserByUserId(UUID clientId, UUID userId) {
        return restTemplate.getForEntity("/users/{userId}", ClientUserDTO.class, userId).getBody();
    }
}

package nl.ing.lovebird.tokens.clients;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.tokens.IntegrationTest;
import nl.ing.lovebird.tokens.clients.cassandra.Client;
import nl.ing.lovebird.tokens.clients.cassandra.ClientRepository;
import nl.ing.lovebird.tokens.clients.events.ClientDeletedEvent;
import nl.ing.lovebird.tokens.clients.events.ClientEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ClientDeletedEventConsumerIT {

    private final UUID clientGroupId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final Client client = new Client(clientId, "clientName", null, clientGroupId, true, true, true, true, true, true, true, true, true, true, false, true, true, true);

    @Value("${yolt.kafka.topics.ycs-client-deleted-events.topic-name}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, ClientEvent> kafkaTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private ClientRepository clientRepository;

    @Test
    void consumeClientDeleteEvent() {
        clientRepository.save(client);

        ClientToken clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        ClientDeletedEvent event = new ClientDeletedEvent(clientId);
        Message<ClientDeletedEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, client.getId().toString())
                .build();
        kafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(clientRepository.getById(client.getId())).contains(new Client(clientId, "clientName", null, clientGroupId, true, true, true, true, true, true, true, true, true, true, true, true, true, true)));
    }
}

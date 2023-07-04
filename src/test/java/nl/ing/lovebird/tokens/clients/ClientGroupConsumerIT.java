package nl.ing.lovebird.tokens.clients;

import nl.ing.lovebird.clienttokens.ClientGroupToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.tokens.IntegrationTest;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroup;
import nl.ing.lovebird.tokens.clients.events.ClientGroupEvent;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@RunWith(SpringRunner.class)
public class ClientGroupConsumerIT {
    private final UUID clientGroupId = UUID.randomUUID();
    private final ClientGroup clientGroup = new ClientGroup(clientGroupId, "client group");

    @Value("${yolt.kafka.topics.ycs-client-group-events.topic-name}")
    private String addChangeTopic;

    @Autowired
    private KafkaTemplate<String, ClientGroupEvent> addChangeKafkaTemplate;
    @Autowired
    private ClientService clientService;
    @Autowired
    private TestClientTokens testClientTokens;

    @Test
    public void testAddClientGroup() {
        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        Message<ClientGroupEvent> message = MessageBuilder
                .withPayload(new ClientGroupEvent(
                        ClientGroupEvent.Action.ADD,
                        clientGroup.getId(),
                        clientGroup.getName()
                ))
                .setHeader(KafkaHeaders.TOPIC, addChangeTopic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientGroup.getId().toString())
                .build();
        addChangeKafkaTemplate.send(message);

        await().untilAsserted(() -> {
            ClientGroup actual;
            try {
                actual = clientService.getClientGroup(clientGroup.getId());
            } catch (ClientGroupNotFoundException ex) {
                actual = null;
            }
            assertThat(actual).isEqualTo(clientGroup);
        });
    }

    @Test
    public void testChangeClientGroup() {
        clientService.saveClientGroup(new ClientGroupEvent(
                ClientGroupEvent.Action.ADD,
                clientGroup.getId(),
                "other name"));

        assertThat(clientService.getClientGroup(clientGroup.getId())).isNotEqualTo(clientGroup);

        ClientGroupToken clientGroupToken = testClientTokens.createClientGroupToken(clientGroupId);
        Message<ClientGroupEvent> message = MessageBuilder
                .withPayload(new ClientGroupEvent(
                        ClientGroupEvent.Action.UPDATE,
                        clientGroup.getId(),
                        clientGroup.getName()
                ))
                .setHeader(KafkaHeaders.TOPIC, addChangeTopic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientGroupToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientGroup.getId().toString())
                .build();
        addChangeKafkaTemplate.send(message);

        await().untilAsserted(() -> {
            ClientGroup actual;
            try {
                actual = clientService.getClientGroup(clientGroup.getId());
            } catch (ClientGroupNotFoundException ex) {
                actual = null;
            }
            assertThat(actual).isEqualTo(clientGroup);
        });
    }
}
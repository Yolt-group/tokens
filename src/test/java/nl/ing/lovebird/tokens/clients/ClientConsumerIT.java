package nl.ing.lovebird.tokens.clients;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.tokens.IntegrationTest;
import nl.ing.lovebird.tokens.clients.cassandra.Client;
import nl.ing.lovebird.tokens.clients.cassandra.ClientRepository;
import nl.ing.lovebird.tokens.clients.events.ClientEvent;
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
public class ClientConsumerIT {
    private final UUID clientGroupId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();
    private final Client client = new Client(clientId, "clientName", null, clientGroupId, true, true, true, true, true, true, true, true, true, true, false, true, true, true);

    @Value("${yolt.kafka.topics.ycs-client-events.topic-name}")
    private String addChangeTopic;

    @Autowired
    private KafkaTemplate<String, ClientEvent> addChangeKafkaTemplate;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private TestClientTokens testClientTokens;

    @After
    public void tearDown() {
        try {
            clientRepository.delete(client);
        } catch (EmptyResultDataAccessException e) {
            // allowed
        }
    }

    @Test
    public void testAddClient() {
        assertThat(clientRepository.getById(client.getId())).isNotPresent();
        ClientEvent event = new ClientEvent(
                ClientEvent.Action.ADD,
                client.getId(),
                client.getClientGroupId(),
                client.getName(),
                client.getCam(),
                client.getPsd2Licensed(),
                client.getAis(),
                client.getPis(),
                new ClientEvent.ClientUsersKyc(client.getClientUsersKycPrivateIndividuals(), client.getClientUsersKycEntities()),
                new ClientEvent.DataEnrichment(client.getDataEnrichmentMerchantRecognition(), client.getDataEnrichmentCategorization(), client.getDataEnrichmentCycleDetection(), client.getDataEnrichmentLabels()),
                client.getConsentStarter(),
                client.getOneOffAis(),
                client.getRiskInsights()
        );

        ClientToken clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        Message<ClientEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, addChangeTopic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, client.getId().toString())
                .build();
        addChangeKafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(clientRepository.getById(client.getId())).contains(client));
    }

    @Test
    public void testChangeClient() {
        clientRepository.save(new Client(client.getId(), "other name", null, client.getClientGroupId(), true, true, true, true, true, true, true, true, true, true, true, true, true, true));
        assertThat(clientRepository.getById(client.getId())).isPresent().get().isNotEqualTo(client);

        ClientEvent event = new ClientEvent(
                ClientEvent.Action.UPDATE,
                client.getId(),
                client.getClientGroupId(),
                client.getName(),
                client.getCam(),
                client.getPsd2Licensed(),
                client.getAis(),
                client.getPis(),
                new ClientEvent.ClientUsersKyc(client.getClientUsersKycPrivateIndividuals(), client.getClientUsersKycEntities()),
                new ClientEvent.DataEnrichment(client.getDataEnrichmentMerchantRecognition(), client.getDataEnrichmentCategorization(), client.getDataEnrichmentCycleDetection(), client.getDataEnrichmentLabels()),
                client.getConsentStarter(),
                client.getOneOffAis(),
                client.getRiskInsights()
        );

        ClientToken clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        Message<ClientEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, addChangeTopic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, client.getId().toString())
                .build();
        addChangeKafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(clientRepository.getById(client.getId())).contains(client));
    }
}

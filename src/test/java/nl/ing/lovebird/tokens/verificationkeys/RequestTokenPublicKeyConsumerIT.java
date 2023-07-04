package nl.ing.lovebird.tokens.verificationkeys;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.tokens.IntegrationTest;
import nl.ing.lovebird.tokens.TestUtil;
import nl.ing.lovebird.tokens.verificationkeys.events.RequestTokenPublicKeyEvent;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
@RunWith(SpringRunner.class)
public class RequestTokenPublicKeyConsumerIT {

    private final String keyId = "request-token-public-key-id";
    private final UUID clientGroupId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Value("${yolt.kafka.topics.ycs-request-token-public-key.topic-name}")
    private String topic;

    @Autowired
    private KafkaTemplate<String, RequestTokenPublicKeyEvent> kafkaTemplate;
    @Autowired
    private TestClientTokens testClientTokens;
    @Autowired
    private VerificationKeyRepository verificationKeyRepository;

    @After
    public void tearDown() {
        try {
            verificationKeyRepository.deleteVerificationKey(clientId, keyId);
        } catch (EmptyResultDataAccessException e) {
            // allowed
        }
    }

    @Test
    public void testAdd() {
        LocalDateTime created = LocalDateTime.of(2020, 1, 13, 0, 0);

        assertThat(verificationKeyRepository.getVerificationKey(clientId, keyId)).isNotPresent();
        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(
                RequestTokenPublicKeyEvent.Action.ADD,
                clientId,
                keyId,
                TestUtil.getDefaultPublicKey(),
                created
        );

        ClientToken clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        Message<RequestTokenPublicKeyEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .build();
        kafkaTemplate.send(message);

        await().untilAsserted(() -> {
            Optional<VerificationKey> savedKey = verificationKeyRepository.getVerificationKey(clientId, keyId);
            assertThat(savedKey).isPresent();
            assertThat(savedKey.get().getKeyId()).isEqualTo(keyId);
            assertThat(savedKey.get().getClientId()).isEqualTo(clientId);
            assertThat(savedKey.get().getVerificationKey()).isEqualTo(TestUtil.getDefaultPublicKey());
        });
    }

    @Test
    public void testDelete() {
        Instant created = ZonedDateTime.of(LocalDateTime.of(2020, 1, 13, 0, 0), ZoneId.systemDefault()).toInstant();
        verificationKeyRepository.putVerificationKey(clientId, keyId, TestUtil.getDefaultPublicKey(), created);

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(
                RequestTokenPublicKeyEvent.Action.DELETE,
                clientId,
                keyId,
                TestUtil.getDefaultPublicKey(),
                LocalDateTime.of(2020, 1, 13, 0, 0)
        );

        ClientToken clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        Message<RequestTokenPublicKeyEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .build();
        kafkaTemplate.send(message);

        await().untilAsserted(() -> assertThat(verificationKeyRepository.getVerificationKey(clientId, keyId)).isEmpty());
    }

    @Test
    public void testUpdate() {
        Instant oldCreated = ZonedDateTime.of(LocalDateTime.of(2020, 1, 13, 0, 0), ZoneId.systemDefault()).toInstant();

        String oldPublicKey = "dummy-key";
        verificationKeyRepository.putVerificationKey(clientId, keyId, oldPublicKey, oldCreated);

        LocalDateTime newCreated = LocalDateTime.of(2020, 1, 15, 0, 0);
        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(
                RequestTokenPublicKeyEvent.Action.UPDATE,
                clientId,
                keyId,
                TestUtil.getDefaultPublicKey(),
                newCreated
        );

        ClientToken clientToken = testClientTokens.createClientToken(clientGroupId, clientId);
        Message<RequestTokenPublicKeyEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientId.toString())
                .build();
        kafkaTemplate.send(message);

        await().untilAsserted(() -> {
            Optional<VerificationKey> savedKey = verificationKeyRepository.getVerificationKey(clientId, keyId);
            assertThat(savedKey).isPresent();
            assertThat(savedKey.get().getCreated()).isEqualTo(ZonedDateTime.of(newCreated, ZoneId.systemDefault()).toInstant());
            assertThat(savedKey.get().getVerificationKey()).isEqualTo(TestUtil.getDefaultPublicKey());
        });
    }
}

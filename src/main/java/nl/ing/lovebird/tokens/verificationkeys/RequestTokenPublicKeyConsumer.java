package nl.ing.lovebird.tokens.verificationkeys;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import nl.ing.lovebird.tokens.verificationkeys.events.RequestTokenPublicKeyEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTokenPublicKeyConsumer {

    private final VerificationKeyRepository verificationKeyRepository;
    private final ClientIdVerificationService clientIdVerificationService;

    @KafkaListener(
            topics = "${yolt.kafka.topics.ycs-request-token-public-key.topic-name}",
            concurrency = "${yolt.kafka.topics.ycs-request-token-public-key.listener-concurrency}"
    )
    public void consume(@Payload @Validated RequestTokenPublicKeyEvent event,
                        @Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, event.getClientId());

        switch (event.getAction()) {
            case ADD:
                if (verificationKeyRepository.getVerificationKey(event.getClientId(), event.getKeyId()).isPresent()) {
                    log.warn("the key with id {} for client {} already exists, not saving it", event.getKeyId(), event.getClientId()); //NOSHERIFF
                } else {
                    Instant created = ZonedDateTime.of(event.getCreated(), ZoneId.systemDefault()).toInstant();
                    verificationKeyRepository.putVerificationKey(event.getClientId(), event.getKeyId(), event.getRequestTokenPublicKey(), created);
                }
                break;
            case DELETE:
                if (verificationKeyRepository.getVerificationKey(event.getClientId(), event.getKeyId()).isEmpty()) {
                    log.warn("the key with id {} for client {} does not exist, not deleting it", event.getKeyId(), event.getClientId()); //NOSHERIFF
                } else {
                    verificationKeyRepository.deleteVerificationKey(event.getClientId(), event.getKeyId());
                }
                break;
            case UPDATE:
                if (verificationKeyRepository.getVerificationKey(event.getClientId(), event.getKeyId()).isPresent()) {
                    Instant created = ZonedDateTime.of(event.getCreated(), ZoneId.systemDefault()).toInstant();
                    verificationKeyRepository.putVerificationKey(event.getClientId(), event.getKeyId(), event.getRequestTokenPublicKey(), created);
                } else {
                    log.warn("the key with id {} for client {} does not exist, not updating it", event.getKeyId(), event.getClientId()); //NOSHERIFF
                }
        }
    }
}

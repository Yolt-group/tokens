package nl.ing.lovebird.tokens.clients;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import nl.ing.lovebird.tokens.clients.cassandra.ClientRepository;
import nl.ing.lovebird.tokens.clients.events.ClientDeletedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.validation.Valid;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientDeletedEventConsumer {

    private final ClientIdVerificationService clientIdVerificationService;
    private final ClientService clientService;

    @SneakyThrows
    @KafkaListener(
            topics = "${yolt.kafka.topics.ycs-client-deleted-events.topic-name}",
            concurrency = "${yolt.kafka.topics.ycs-client-deleted-events.listener-concurrency}"
    )
    public void consumeClientDeletedEvent(@Payload @Valid ClientDeletedEvent event,
                                          @Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken) {
        clientIdVerificationService.verify(clientToken, event.getClientId());
        clientService.deleteClient(clientToken);
    }

}

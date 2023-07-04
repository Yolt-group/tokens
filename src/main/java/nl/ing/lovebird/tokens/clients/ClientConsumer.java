package nl.ing.lovebird.tokens.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.tokens.clients.events.ClientEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientConsumer {
    private final ClientService clientService;

    @KafkaListener(
            topics = "${yolt.kafka.topics.ycs-client-events.topic-name}",
            concurrency = "${yolt.kafka.topics.ycs-client-events.listener-concurrency}"
    )
    public void consume(@Payload @Validated ClientEvent event) {

        switch (event.getAction()) {
            case ADD: // fallthrough, upsert
            case UPDATE: // fallthrough, upsert
            case SYNC:  clientService.saveClient(event);
        }
    }
}

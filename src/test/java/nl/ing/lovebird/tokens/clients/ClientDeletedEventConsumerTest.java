package nl.ing.lovebird.tokens.clients;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import nl.ing.lovebird.tokens.clients.events.ClientDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClientDeletedEventConsumerTest {

    @Mock
    private ClientIdVerificationService clientIdVerificationService;
    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientDeletedEventConsumer clientDeletedEventConsumer;

    @Mock
    private ClientToken clientToken;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
    }

    @Test
    void consumeClientDeleteEvent() {
        clientDeletedEventConsumer.consumeClientDeletedEvent(new ClientDeletedEvent(clientId), clientToken);

        verify(clientIdVerificationService).verify(clientToken, clientId);
        verify(clientService).deleteClient(clientToken);

    }
}

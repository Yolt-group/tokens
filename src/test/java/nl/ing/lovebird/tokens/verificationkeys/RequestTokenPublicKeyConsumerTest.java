package nl.ing.lovebird.tokens.verificationkeys;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.verification.ClientIdVerificationService;
import nl.ing.lovebird.tokens.verificationkeys.events.RequestTokenPublicKeyEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RequestTokenPublicKeyConsumerTest {

    private RequestTokenPublicKeyConsumer requestTokenPublicKeyConsumer;

    @Mock
    private VerificationKeyRepository verificationKeyRepository;

    @Mock
    private ClientIdVerificationService clientIdVerificationService;

    @Mock
    private ClientToken clientToken;

    @BeforeEach
    void setUp() {
        requestTokenPublicKeyConsumer = new RequestTokenPublicKeyConsumer(verificationKeyRepository, clientIdVerificationService);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(verificationKeyRepository);
    }

    @Test
    void addNewRequestTokenPublicKey() {
        UUID clientId = UUID.randomUUID();
        String keyId = UUID.randomUUID().toString();
        String key = "random key";

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        Instant createdInstant = ZonedDateTime.of(created, ZoneId.systemDefault()).toInstant();

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.ADD, clientId, keyId, key, created);
        requestTokenPublicKeyConsumer.consume(event, clientToken);

        verify(verificationKeyRepository).getVerificationKey(clientId, keyId);
        verify(verificationKeyRepository).putVerificationKey(clientId, keyId, key, createdInstant);
    }

    @Test
    void addNewRequestTokenPublicKeyExists() {
        UUID clientId = UUID.randomUUID();
        String keyId = UUID.randomUUID().toString();
        String key = "random key";

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        Instant createdInstant = ZonedDateTime.of(created, ZoneId.systemDefault()).toInstant();

        when(verificationKeyRepository.getVerificationKey(clientId, keyId)).thenReturn(Optional.of(new VerificationKey(clientId, keyId, key, createdInstant)));

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.ADD, clientId, keyId, key, created);
        requestTokenPublicKeyConsumer.consume(event, clientToken);

        verify(verificationKeyRepository, never()).putVerificationKey(clientId, keyId, key, createdInstant);
    }

    @Test
    void deleteRequestTokenPublicKey() {
        UUID clientId = UUID.randomUUID();
        String keyId = UUID.randomUUID().toString();
        String key = "random key";

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        Instant createdInstant = ZonedDateTime.of(created, ZoneId.systemDefault()).toInstant();

        when(verificationKeyRepository.getVerificationKey(clientId, keyId)).thenReturn(Optional.of(new VerificationKey(clientId, keyId, key, createdInstant)));

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.DELETE, clientId, keyId, key, created);
        requestTokenPublicKeyConsumer.consume(event, clientToken);

        verify(verificationKeyRepository).getVerificationKey(clientId, keyId);
        verify(verificationKeyRepository).deleteVerificationKey(clientId, keyId);
    }

    @Test
    void deleteRequestTokenPublicKeyNotFound() {
        UUID clientId = UUID.randomUUID();
        String keyId = UUID.randomUUID().toString();
        String key = "random key";

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0, 0);

        when(verificationKeyRepository.getVerificationKey(clientId, keyId)).thenReturn(Optional.empty());

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.DELETE, clientId, keyId, key, created);
        requestTokenPublicKeyConsumer.consume(event, clientToken);

        verify(verificationKeyRepository, never()).deleteVerificationKey(clientId, keyId);
    }

    @Test
    void updateRequestTokenPublicKeyExists() {
        UUID clientId = UUID.randomUUID();
        String keyId = UUID.randomUUID().toString();
        String key = "random key";

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        Instant createdInstant = ZonedDateTime.of(created, ZoneId.systemDefault()).toInstant();

        when(verificationKeyRepository.getVerificationKey(clientId, keyId)).thenReturn(Optional.of(new VerificationKey(clientId, keyId, key, createdInstant)));

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.UPDATE, clientId, keyId, key, created);
        requestTokenPublicKeyConsumer.consume(event, clientToken);

        verify(verificationKeyRepository).getVerificationKey(clientId, keyId);
        verify(verificationKeyRepository).putVerificationKey(clientId, keyId, key, createdInstant);
    }

    @Test
    void updateRequestTokenPublicKeyDoesNotExist() {
        UUID clientId = UUID.randomUUID();
        String keyId = UUID.randomUUID().toString();
        String key = "random key";

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        Instant createdInstant = ZonedDateTime.of(created, ZoneId.systemDefault()).toInstant();

        when(verificationKeyRepository.getVerificationKey(clientId, keyId)).thenReturn(Optional.empty());

        RequestTokenPublicKeyEvent event = new RequestTokenPublicKeyEvent(RequestTokenPublicKeyEvent.Action.UPDATE, clientId, keyId, key, created);
        requestTokenPublicKeyConsumer.consume(event, clientToken);

        verify(verificationKeyRepository, never()).putVerificationKey(clientId, keyId, key, createdInstant);
    }
}

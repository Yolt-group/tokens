package nl.ing.lovebird.tokens.verificationkeys;

import nl.ing.lovebird.tokens.IntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nl.ing.lovebird.tokens.TestUtil.getDefaultPublicKey;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@IntegrationTest
public class VerificationKeyRepositoryIT {

    private final UUID clientId = UUID.randomUUID();
    private final Instant created = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 0, 0, 0), ZoneId.systemDefault()).toInstant();

    @Autowired
    private VerificationKeyRepository verificationKeyRepository;

    @Test
    public void getVerificationKeys_whenNotPresent_shouldReturnEmptyList() {
        List<VerificationKey> verificationKeys = verificationKeyRepository.getVerificationKeys(clientId);

        assertThat(verificationKeys).isEmpty();
    }

    @Test
    public void getVerificationKeys_shouldReturnForAllClients() {
        VerificationKey vk1 = new VerificationKey(UUID.randomUUID(), "keyId1", getDefaultPublicKey(), Instant.now());
        VerificationKey vk2 = new VerificationKey(UUID.randomUUID(), "keyId2", getDefaultPublicKey(), Instant.now());

        verificationKeyRepository.putVerificationKey(vk1.getClientId(), vk1.getKeyId(), vk1.getVerificationKey(), created);
        verificationKeyRepository.putVerificationKey(vk2.getClientId(), vk2.getKeyId(), vk2.getVerificationKey(), created);

        List<VerificationKey> newVerificationKeys = verificationKeyRepository.getVerificationKeys();

        assertThat(newVerificationKeys.size()).isGreaterThan(2);
        assertThat(newVerificationKeys)
                .extracting(VerificationKey::getClientId)
                .contains(vk1.getClientId(), vk2.getClientId());
        assertThat(newVerificationKeys)
                .extracting(VerificationKey::getKeyId)
                .contains(vk1.getKeyId(), vk2.getKeyId());
        assertThat(newVerificationKeys)
                .extracting(VerificationKey::getVerificationKey)
                .contains(vk1.getVerificationKey());
    }

    @Test
    public void getVerificationKeys_whenPresent_shouldReturnAsList() {
        verificationKeyRepository.putVerificationKey(clientId, "keyId1", getDefaultPublicKey(), created);
        verificationKeyRepository.putVerificationKey(clientId, "keyId2", getDefaultPublicKey(), created);

        List<VerificationKey> clientKeys = verificationKeyRepository.getVerificationKeys(clientId);

        assertThat(clientKeys)
                .extracting(VerificationKey::getClientId)
                .containsOnly((clientId));
        assertThat(clientKeys)
                .extracting(VerificationKey::getVerificationKey)
                .containsOnly(getDefaultPublicKey());
        assertThat(clientKeys)
                .extracting(VerificationKey::getKeyId)
                .containsExactlyInAnyOrder("keyId1", "keyId2");
        assertThat(clientKeys)
                .extracting(VerificationKey::getCreated)
                .containsOnly(created);
    }

    @Test
    public void getVerificationKey_whenNotPresent_shouldReturnOptionalEmpty() {
        Optional<VerificationKey> verificationKeyOptional = verificationKeyRepository.getVerificationKey(clientId, "keyId");

        assertThat(verificationKeyOptional).isNotPresent();
    }

    @Test
    public void putVerificationKey_shouldInsertAndGetEntry() {
        verificationKeyRepository.putVerificationKey(clientId, "keyId", getDefaultPublicKey(), created);

        Optional<VerificationKey> verificationKeyOptional = verificationKeyRepository.getVerificationKey(clientId, "keyId");

        assertThat(verificationKeyOptional.get().getClientId()).isEqualTo(clientId);
        assertThat(verificationKeyOptional.get().getKeyId()).isEqualTo("keyId");
        assertThat(verificationKeyOptional.get().getVerificationKey()).isEqualTo(getDefaultPublicKey());
        assertThat(verificationKeyOptional.get().getCreated()).isEqualTo(created);
    }

    @Test
    public void deleteVerificationKey_whenEntryIsPresent_shouldDeleteIt() {
        VerificationKey verificationKey = new VerificationKey(clientId, "keyId", getDefaultPublicKey(), Instant.now());
        verificationKeyRepository.putVerificationKey(verificationKey.getClientId(), verificationKey.getKeyId(), verificationKey.getVerificationKey(), created);

        verificationKeyRepository.deleteVerificationKey(verificationKey.getClientId(), verificationKey.getKeyId());

        Optional<VerificationKey> verificationKeyOptional = verificationKeyRepository.getVerificationKey(verificationKey.getClientId(), verificationKey.getKeyId());
        assertThat(verificationKeyOptional).isNotPresent();
    }
}

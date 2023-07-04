package nl.ing.lovebird.tokens.verificationkeys;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.tokens.exception.VerificationKeyAlreadyStoredException;
import nl.ing.lovebird.tokens.exception.VerificationKeyNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VerificationKeyService {

    private final VerificationKeyRepository verificationKeyRepository;
    private final int verificationKeyTillNotificationInDays;
    private final int verificationKeyValidityInDays;

    VerificationKeyService(VerificationKeyRepository verificationKeyRepository,
                           @Value("${verification-key.till-notification-in-days:304}") int verificationKeyTillNotificationInDays,
                           @Value("${verification-key.validity-in-days:365}") int verificationKeyValidityInDays) {
        this.verificationKeyRepository = verificationKeyRepository;
        this.verificationKeyTillNotificationInDays = verificationKeyTillNotificationInDays;
        this.verificationKeyValidityInDays = verificationKeyValidityInDays;
    }

    List<VerificationKeyDTO> getVerificationKeys() {
        return verificationKeyRepository.getVerificationKeys().stream()
                .map(this::mapFrom)
                .collect(Collectors.toList());
    }

    List<VerificationKeyDTO> getVerificationKeys(UUID clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("ClientId should not be null");
        }

        return verificationKeyRepository.getVerificationKeys(clientId).stream()
                .map(this::mapFrom)
                .collect(Collectors.toList());
    }

    public VerificationKey getVerificationKey(String kid, UUID clientId) {
        return getVerificationKey(clientId, kid).orElseThrow(() ->
                new VerificationKeyNotFoundException("No verification keys found for clientId " + clientId + " and keyId " + kid));
    }

    private Optional<VerificationKey> getVerificationKey(@NotNull final UUID clientId, final String keyId) {
        if (StringUtils.isBlank(keyId)) {
            log.info("KeyId (kid) was not provided for client with ID {} when requesting access token, using fallback keyId", clientId);
            return verificationKeyRepository.getVerificationKey(clientId, VerificationKey.FALLBACK_KEY);
        } else {
            return verificationKeyRepository.getVerificationKey(clientId, keyId);
        }
    }

    private VerificationKeyDTO mapFrom(@NotNull VerificationKey verificationKey) {
        return new VerificationKeyDTO(verificationKey.getClientId(), verificationKey.getKeyId(),
                verificationKey.getVerificationKey(), verificationKey.getCreated(),
                verificationKey.getCreated().plus(verificationKeyTillNotificationInDays, ChronoUnit.DAYS),
                verificationKey.getCreated().plus(verificationKeyValidityInDays, ChronoUnit.DAYS));
    }
}

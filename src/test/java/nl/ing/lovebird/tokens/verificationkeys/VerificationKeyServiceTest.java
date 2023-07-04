package nl.ing.lovebird.tokens.verificationkeys;

import nl.ing.lovebird.tokens.exception.VerificationKeyNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VerificationKeyServiceTest {

    private static final UUID DEFAULT_UUID = UUID.randomUUID();

    private VerificationKeyService verificationKeyService;

    @Mock
    private VerificationKeyRepository verificationKeyRepositoryMock;

    @Before
    public void setUp() {
        verificationKeyService = new VerificationKeyService(verificationKeyRepositoryMock, 10, 20);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(verificationKeyRepositoryMock);
    }

    @Test
    public void getVerificationKeys_shouldCallRepository() {
        verificationKeyService.getVerificationKeys();

        verify(verificationKeyRepositoryMock).getVerificationKeys();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getVerificationKeys_whenClientIdNull_shouldThrowException() {
        verificationKeyService.getVerificationKeys(null);
    }

    @Test
    public void getVerificationKeys_whenProperClientId_shouldCallRepository() {
        verificationKeyService.getVerificationKeys(DEFAULT_UUID);

        verify(verificationKeyRepositoryMock).getVerificationKeys(eq(DEFAULT_UUID));
    }


    @Test(expected = VerificationKeyNotFoundException.class)
    public void getVerificationKey_whenNoResultFromDb_shouldThrowException() throws Exception {
        String keyId = "some_kid";
        when(verificationKeyRepositoryMock.getVerificationKey(DEFAULT_UUID, keyId)).thenReturn(Optional.empty());

        try {
            verificationKeyService.getVerificationKey(keyId, DEFAULT_UUID);
        } finally {
            verify(verificationKeyRepositoryMock).getVerificationKey(eq(DEFAULT_UUID), eq(keyId));
        }
    }

    @Test
    public void getVerificationKey_whenNoKid_shouldFallbackToDefault() throws Exception {
        when(verificationKeyRepositoryMock.getVerificationKey(any(), anyString())).thenReturn(Optional.of(new VerificationKey()));

        verificationKeyService.getVerificationKey(null, DEFAULT_UUID);

        verify(verificationKeyRepositoryMock).getVerificationKey(eq(DEFAULT_UUID), eq(VerificationKey.FALLBACK_KEY));
    }

    @Test
    public void getVerificationKey_whenKidProvided_shouldUseItForDbSearch() throws Exception {
        String providedKeyId = "provided_kid";
        when(verificationKeyRepositoryMock.getVerificationKey(any(), anyString())).thenReturn(Optional.of(new VerificationKey()));

        verificationKeyService.getVerificationKey(providedKeyId, DEFAULT_UUID);

        verify(verificationKeyRepositoryMock).getVerificationKey(eq(DEFAULT_UUID), eq(providedKeyId));
    }
}

package nl.ing.lovebird.tokens.authentication;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.tokens.IntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@IntegrationTest
@RunWith(SpringRunner.class)
public class RequestTokenNonceRepositoryIT {

    @Autowired
    private RequestTokenNonceRepository requestTokenNonceRepository;

    @Autowired
    private Session session;

    @Test
    public void shouldReturnANonce() {
        UUID existingNonce = UUID.randomUUID();
        session.execute("INSERT INTO request_token_nonce(uuid) VALUES (" + existingNonce + ");");
        Optional<RequestTokenNonce> requestTokenNonce = requestTokenNonceRepository.getById(existingNonce);

        assertTrue(requestTokenNonce.isPresent());
        assertEquals(existingNonce, requestTokenNonce.get().getUuid());
    }

    @Test
    public void shouldNotReturnANonce() {
        Optional<RequestTokenNonce> nonce = requestTokenNonceRepository.getById(UUID.randomUUID());
        assertFalse(nonce.isPresent());
    }

    @Test
    public void shouldSaveANonce() {
        UUID uuid = UUID.randomUUID();
        requestTokenNonceRepository.save(new RequestTokenNonce(uuid));
        Optional<RequestTokenNonce> nonce = requestTokenNonceRepository.getById(uuid);
        assertTrue(nonce.isPresent());
    }
}
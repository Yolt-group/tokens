package nl.ing.lovebird.tokens.authentication;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.cassandra.CassandraRepository;
import nl.ing.lovebird.logging.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.mapping.Mapper.Option.ttl;

@Repository
public class RequestTokenNonceRepository extends CassandraRepository<RequestTokenNonce> {

    @Value("${request-token.validity-in-sec}")
    private int expirationTimeInSec;

    @Autowired
    public RequestTokenNonceRepository(Session session) {
        super(session, RequestTokenNonce.class);
    }

    public Optional<RequestTokenNonce> getById(final UUID id) {

        return selectOne(eq(RequestTokenNonce.ID_COLUMN, id));
    }

    @Override
    public void save(final RequestTokenNonce requestTokenNonce) {
        try {
            mapper.save(requestTokenNonce, ttl(expirationTimeInSec + RequestTokenIssuedAtValidator.ALLOWED_CLOCK_SKEW_IN_SECONDS));
            AuditLogger.logSuccess("Saved Nonce for " + expirationTimeInSec + " sec", requestTokenNonce);
        } catch (RuntimeException e) {
            AuditLogger.logError("Error saving Nonce ", requestTokenNonce, e);
            throw e;
        }
    }

}

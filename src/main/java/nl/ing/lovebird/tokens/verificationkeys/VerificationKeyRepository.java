package nl.ing.lovebird.tokens.verificationkeys;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
public
class VerificationKeyRepository extends CassandraRepository<VerificationKey> {

    @Autowired
    VerificationKeyRepository(Session session) {
        super(session, VerificationKey.class);
    }

    public List<VerificationKey> getVerificationKeys() {
        return select(createSelect());
    }

    public List<VerificationKey> getVerificationKeys(final UUID clientId) {
        return select(eq(VerificationKey.ID_COLUMN, clientId));
    }

    Optional<VerificationKey> getVerificationKey(final UUID clientId, final String keyId) {
        return selectOne(createSelect()
                .where(eq(VerificationKey.ID_COLUMN, clientId))
                .and(eq(VerificationKey.KEY_ID_COLUMN, keyId)));
    }

    public void putVerificationKey(final UUID clientId, final String keyId, final String verificationKey, Instant created) {
        final Insert insert = createInsert()
                .value(VerificationKey.ID_COLUMN, clientId)
                .value(VerificationKey.KEY_ID_COLUMN, keyId)
                .value(VerificationKey.VERIFICATION_KEY_COLUMN, verificationKey)
                .value(VerificationKey.CREATED_COLUMN, created);

        executeInsert(insert);
    }

    public void deleteVerificationKey(final UUID clientId, final String keyId) {
        final Delete delete = createDelete();
        delete.where(eq(VerificationKey.ID_COLUMN, clientId))
                .and(eq(VerificationKey.KEY_ID_COLUMN, keyId));

        executeDelete(delete);
    }
}

package nl.ing.lovebird.tokens.clients.cassandra;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository("ClientGroupCassandraRepository")
public class ClientGroupRepository extends CassandraRepository<ClientGroup> {

    @Autowired
    public ClientGroupRepository(Session session) {
        super(session, ClientGroup.class);
    }

    @Override
    public void save(ClientGroup entity) {
        super.save(entity);
    }

    @Override
    public void delete(ClientGroup entity) {
        super.delete(entity);
    }

    public Optional<ClientGroup> getById(@NotNull final UUID clientGroupId) {
        return selectOne(eq(ClientGroup.ID_COLUMN, clientGroupId));
    }

    public List<ClientGroup> findAll() {
        return select(createSelect());
    }
}

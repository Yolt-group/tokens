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

@Repository("ClientCassandraRepository")
public class ClientRepository extends CassandraRepository<Client> {

    @Autowired
    public ClientRepository(Session session) {
        super(session, Client.class);
    }

    @Override
    public void save(Client entity) {
        super.save(entity);
    }

    @Override
    public void delete(Client entity) {
        super.delete(entity);
    }

    public Optional<Client> getById(@NotNull final UUID clientId) {
        return selectOne(eq(Client.ID_COLUMN, clientId));
    }

    public List<Client> findAll() {
        return select(createSelect());
    }

}

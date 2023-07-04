package nl.ing.lovebird.tokens.clientcertificates;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
class ClientProxyClientCertificateRepository extends CassandraRepository<ClientProxyClientCertificate> {

    @Autowired
    ClientProxyClientCertificateRepository(Session session) {
        super(session, ClientProxyClientCertificate.class);
    }

    List<ClientProxyClientCertificate> getClientCertificates(final UUID clientId) {
        return select(eq(ClientProxyClientCertificate.CLIENT_ID_COLUMN, clientId));
    }

    @Override
    protected void save(ClientProxyClientCertificate entity) {
        super.save(entity);
    }
}

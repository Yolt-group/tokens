package nl.ing.lovebird.tokens.clients.postgres;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientGroupRepository extends CrudRepository<ClientGroup, UUID> {
}

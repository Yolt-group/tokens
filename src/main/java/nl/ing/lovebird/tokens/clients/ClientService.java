package nl.ing.lovebird.tokens.clients;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.tokens.clients.cassandra.Client;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroup;
import nl.ing.lovebird.tokens.clients.dto.ClientDTO;
import nl.ing.lovebird.tokens.clients.dto.ClientGroupDTO;
import nl.ing.lovebird.tokens.clients.events.ClientEvent;
import nl.ing.lovebird.tokens.clients.events.ClientGroupEvent;
import nl.ing.lovebird.tokens.clients.postgres.ClientGroupRepository;
import nl.ing.lovebird.tokens.clients.postgres.ClientRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
public class ClientService {
    private final nl.ing.lovebird.tokens.clients.cassandra.ClientRepository cassandraRepository;
    private final ClientRepository clientRepository;
    private final nl.ing.lovebird.tokens.clients.cassandra.ClientGroupRepository cassandraGroupRepository;
    private final ClientGroupRepository clientGroupRepository;

    public ClientService(@Qualifier("ClientCassandraRepository") nl.ing.lovebird.tokens.clients.cassandra.ClientRepository cassandraRepository,
                         ClientRepository clientRepository,
                         @Qualifier("ClientGroupCassandraRepository") nl.ing.lovebird.tokens.clients.cassandra.ClientGroupRepository cassandraGroupRepository,
                         ClientGroupRepository clientGroupRepository) {
        this.cassandraRepository = cassandraRepository;
        this.clientRepository = clientRepository;
        this.cassandraGroupRepository = cassandraGroupRepository;
        this.clientGroupRepository = clientGroupRepository;
    }

    public ClientDTO getClient(final UUID clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client id should not be null");
        }

        return cassandraRepository.getById(clientId)
                .map(client -> toClientDTO(client, getClientGroup(client.getClientGroupId())))
                .orElseThrow(() -> new ClientNotFoundException(clientId));
    }

    public ClientGroup getClientGroup(UUID clientGroupId) {
        if (clientGroupId == null) {
            throw new IllegalArgumentException("ClientGroupId should not be null");
        }

        return cassandraGroupRepository.getById(clientGroupId)
                .orElseThrow(() -> new ClientGroupNotFoundException(clientGroupId));
    }

    private ClientDTO toClientDTO(Client client, ClientGroup clientGroup) {
        return new ClientDTO(
                new ClientGroupDTO(clientGroup.getId(), clientGroup.getName()),
                client.getId(),
                client.getName(),
                client.getClientUsersKycPrivateIndividuals(),
                client.getClientUsersKycEntities(),
                client.getCam(),
                client.getPsd2Licensed(),
                client.getAis(),
                client.getPis(),
                client.getDataEnrichmentMerchantRecognition(),
                client.getDataEnrichmentCategorization(),
                client.getDataEnrichmentCycleDetection(),
                client.getDataEnrichmentLabels(),
                client.getDeleted(),
                client.getConsentStarter(),
                client.getOneOffAis(),
                client.getRiskInsights()
        );
    }

    public ClientDTO saveClient(ClientEvent event) {
        Client client = Client.from(event);
        cassandraRepository.save(client);

        ClientGroup clientGroup = cassandraGroupRepository.getById(event.getClientGroupId())
                .orElse(new ClientGroup(event.getClientGroupId(), ""));

        if (!clientGroupRepository.existsById(clientGroup.getId())) {
            clientGroupRepository.save(new nl.ing.lovebird.tokens.clients.postgres.ClientGroup(clientGroup.getId(), clientGroup.getName(), Collections.emptySet()));
        }

        clientRepository.save(nl.ing.lovebird.tokens.clients.postgres.Client.from(event));

        return toClientDTO(client, clientGroup);
    }

    public void deleteClient(ClientToken clientToken) {
        cassandraRepository.getById(clientToken.getClientIdClaim()).ifPresent(client -> {
            client.setDeleted(true);
            cassandraRepository.save(client);
        });
    }

    public void saveClientGroup(ClientGroupEvent event) {
        ClientGroup clientGroup = new ClientGroup(event.getClientGroupId(), event.getName());
        cassandraGroupRepository.save(clientGroup);

        nl.ing.lovebird.tokens.clients.postgres.ClientGroup clientGroupEntity = clientGroupRepository.findById(clientGroup.getId())
                .orElseGet(() -> new nl.ing.lovebird.tokens.clients.postgres.ClientGroup(clientGroup.getId(), clientGroup.getName(), Collections.emptySet()));
        clientGroupEntity.setName(event.getName());

        clientGroupRepository.save(clientGroupEntity);
    }
}

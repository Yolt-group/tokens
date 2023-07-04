package nl.ing.lovebird.tokens.clients.postgres;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "client_group")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientGroup {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "name")
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "client_group_id", nullable = false, insertable = false, updatable = false)
    private Set<Client> clients;
}

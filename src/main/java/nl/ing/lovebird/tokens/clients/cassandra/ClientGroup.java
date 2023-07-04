package nl.ing.lovebird.tokens.clients.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = ClientGroup.TABLE_NAME)
public class ClientGroup {

    public static final String TABLE_NAME = "clientgroups";
    public static final String ID_COLUMN = "id";
    public static final String NAME_COLUMN = "name";

    @PartitionKey
    @Column(name = ID_COLUMN)
    private UUID id;

    @Column(name = NAME_COLUMN)
    private String name;
}

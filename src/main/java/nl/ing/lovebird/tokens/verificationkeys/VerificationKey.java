package nl.ing.lovebird.tokens.verificationkeys;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Data
@NoArgsConstructor
@Table(name = VerificationKey.TABLE_NAME)
public class VerificationKey {

    public static final String TABLE_NAME = "client_verification_key";
    public static final String ID_COLUMN = "client_id";
    public static final String KEY_ID_COLUMN = "key_id";
    public static final String VERIFICATION_KEY_COLUMN = "verification_key";
    public static final String CREATED_COLUMN = "created";

    public static final String FALLBACK_KEY = "migrated_initial_key";

    @PartitionKey
    @Column(name = ID_COLUMN)
    private UUID clientId;

    @ClusteringColumn
    @Column(name = KEY_ID_COLUMN)
    private String keyId;

    @Column(name = VERIFICATION_KEY_COLUMN)
    private String verificationKey;

    @Column(name = CREATED_COLUMN)
    private Instant created;

    public VerificationKey(UUID clientId, String keyId, String verificationKey, Instant created) {
        this.clientId = clientId;
        this.keyId = keyId;
        this.verificationKey = verificationKey;
        this.created = created;
    }

    public Instant getCreated() {
        if (created != null) {
            return created;
        }
        // Default value (column was created later). Thank you cassandra. Doesn't need to be correct by the hour (hence UTC conversion),
        // but some default value 'roughly' at when the column was created should be returned.
        return LocalDateTime.of(2019, 2, 21, 0, 0).toInstant(ZoneOffset.UTC);
    }

}

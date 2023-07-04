package nl.ing.lovebird.tokens.authentication;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = RequestTokenNonce.TABLE_NAME)
public class RequestTokenNonce {

    public static final String TABLE_NAME = "request_token_nonce";
    public static final String ID_COLUMN = "uuid";

    @PartitionKey
    @Column(name = ID_COLUMN)
    private UUID uuid;

}

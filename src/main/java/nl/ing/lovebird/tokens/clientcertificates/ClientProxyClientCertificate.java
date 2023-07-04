package nl.ing.lovebird.tokens.clientcertificates;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = ClientProxyClientCertificate.TABLE_NAME)
public class ClientProxyClientCertificate {

    public static final String TABLE_NAME = "client_proxy_client_certificates";
    public static final String CLIENT_ID_COLUMN = "client_id";
    public static final String SERIALNUMBER_COLUMN = "serialnumber";
    public static final String CERTIFICATE_COLUMN = "certificate";
    public static final String SUBJECT_COLUMN = "subject";
    public static final String CSR_COLUMN = "csr";
    public static final String VALID_TO_COLUMN = "valid_to";
    public static final String CREATED_COLUMN = "created";

    @PartitionKey
    @Column(name = CLIENT_ID_COLUMN)
    private UUID clientId;

    @ClusteringColumn
    @Column(name = SERIALNUMBER_COLUMN)
    private String serialNumber;

    @Column(name = CERTIFICATE_COLUMN)
    private String certificate;

    @Column(name = SUBJECT_COLUMN)
    private String subject;

    @Column(name = CSR_COLUMN)
    private String csr;

    @Column(name = VALID_TO_COLUMN)
    private Instant validTo;

    @Column(name = CREATED_COLUMN)
    private Instant created;
}

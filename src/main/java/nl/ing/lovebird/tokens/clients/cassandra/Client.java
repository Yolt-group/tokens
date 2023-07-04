package nl.ing.lovebird.tokens.clients.cassandra;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.tokens.clients.events.ClientEvent;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Client.TABLE_NAME)
public class Client {

    public static final String TABLE_NAME = "clients";
    public static final String ID_COLUMN = "id";
    public static final String NAME_COLUMN = "name";
    public static final String CONTACT_ADDRESSES_COLUMN = "contact_addresses";
    public static final String CLIENT_GROUP_ID_COLUMN = "client_group_id";
    public static final String CLIENT_USERS_KYC_PRIVATE_INDIVIDUALS_COLUMN = "client_users_kyc_private_individuals";
    public static final String CLIENT_USERS_KYC_ENTITIES_COLUMN = "client_users_kyc_entities";
    public static final String CAM_COLUMN = "cam";
    public static final String PSD2_LICENSED_COLUMN = "psd2_licensed";
    public static final String AIS_COLUMN = "ais";
    public static final String PIS_COLUMN = "pis";
    public static final String DATA_ENRICHMENT_MERCHANT_RECOGNITION_COLUMN = "data_enrichment_merchant_recognition";
    public static final String DATA_ENRICHMENT_CATEGORIZATION_COLUMN = "data_enrichment_categorization";
    public static final String DATA_ENRICHMENT_CYCLE_DETECTION_COLUMN = "data_enrichment_cycle_detection";
    public static final String DATA_ENRICHMENT_LABELS_COLUMN = "data_enrichment_labels";
    public static final String DELETED_COLUMN = "deleted";
    public static final String AIS_CONSENT_STARTER_COLUMN = "ais_consent_starter";
    public static final String ONE_OFF_AIS = "one_off_ais";
    public static final String RISK_INSIGHTS = "risk_insights";

    @PartitionKey
    @Column(name = ID_COLUMN)
    private UUID id;

    @Column(name = NAME_COLUMN)
    private String name;

    @Column(name = CONTACT_ADDRESSES_COLUMN)
    private Set<String> contactAddresses;

    @Column(name = CLIENT_GROUP_ID_COLUMN)
    private UUID clientGroupId;

    @Column(name = CLIENT_USERS_KYC_PRIVATE_INDIVIDUALS_COLUMN)
    private Boolean clientUsersKycPrivateIndividuals;

    @Column(name = CLIENT_USERS_KYC_ENTITIES_COLUMN)
    private Boolean clientUsersKycEntities;

    @Column(name = CAM_COLUMN)
    private Boolean cam;

    @Column(name = PSD2_LICENSED_COLUMN)
    private Boolean psd2Licensed;

    @Column(name = AIS_COLUMN)
    private Boolean ais;

    @Column(name = PIS_COLUMN)
    private Boolean pis;

    @Column(name = DATA_ENRICHMENT_MERCHANT_RECOGNITION_COLUMN)
    private Boolean dataEnrichmentMerchantRecognition;

    @Column(name = DATA_ENRICHMENT_CATEGORIZATION_COLUMN)
    private Boolean dataEnrichmentCategorization;

    @Column(name = DATA_ENRICHMENT_CYCLE_DETECTION_COLUMN)
    private Boolean dataEnrichmentCycleDetection;

    @Column(name = DATA_ENRICHMENT_LABELS_COLUMN)
    private Boolean dataEnrichmentLabels;

    @Column(name = DELETED_COLUMN)
    private Boolean deleted;

    @Column(name = AIS_CONSENT_STARTER_COLUMN)
    private Boolean consentStarter;

    @Column(name = ONE_OFF_AIS)
    private Boolean oneOffAis;

    @Column(name = RISK_INSIGHTS)
    private Boolean riskInsights;

    public Client(UUID id, String name, Set<String> contactAddresses, UUID clientGroupId) {
        this.id = id;
        this.name = name;
        this.contactAddresses = contactAddresses;
        this.clientGroupId = clientGroupId;
    }

    public Client(UUID clientId, UUID clientGroupId) {
        this.id = clientId;
        this.clientGroupId = clientGroupId;
    }

    public static Client from(ClientEvent event) {
        var client = new Client();
        client.setId(event.getClientId());
        client.setClientGroupId(event.getClientGroupId());
        client.update(event);
        return client;
    }

    public void update(ClientEvent event) {
        this.setName(event.getName());
        this.setClientUsersKycPrivateIndividuals(event.getClientUsersKyc().isPrivateIndividuals());
        this.setClientUsersKycEntities(event.getClientUsersKyc().isEntities());
        this.setPsd2Licensed(event.isPsd2Licensed());
        this.setAis(event.isAis());
        this.setPis(event.isPis());
        this.setCam(event.isCam());
        this.setDataEnrichmentMerchantRecognition(event.getDataEnrichment().isMerchantRecognition());
        this.setDataEnrichmentCategorization(event.getDataEnrichment().isCategorization());
        this.setDataEnrichmentCycleDetection(event.getDataEnrichment().isCycleDetection());
        this.setDataEnrichmentLabels(event.getDataEnrichment().isLabels());
        this.setDeleted(false);
        this.setConsentStarter(event.isConsentStarter());
        this.setOneOffAis(event.isOneOffAis());
        this.setRiskInsights(event.isRiskInsights());
    }
}

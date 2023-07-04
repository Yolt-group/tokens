package nl.ing.lovebird.tokens.clients.postgres;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.ing.lovebird.tokens.clients.cassandra.ClientGroup;
import nl.ing.lovebird.tokens.clients.events.ClientEvent;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "client")
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class Client {

    @Id
    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "client_group_id")
    private UUID clientGroupId;

    @Column(name = "name")
    private String name;

    @Column(name = "kyc_private_individuals")
    private boolean kycPrivateIndividuals;

    @Column(name = "kyc_entities")
    private boolean kycEntities;

    @Column(name = "data_enrichment_merchant_recognition")
    private boolean dataEnrichmentMerchantRecognition;

    @Column(name = "data_enrichment_categorization")
    private boolean dataEnrichmentCategorization;

    @Column(name = "data_enrichment_cycle_detection")
    private boolean dataEnrichmentCycleDetection;

    @Column(name = "data_enrichment_labels")
    private boolean dataEnrichmentLabels;

    @Column(name = "cam")
    private boolean cam;

    @Column(name = "psd2_licensed")
    private boolean psd2Licensed;

    @Column(name = "ais")
    private boolean ais;

    @Column(name = "pis")
    private boolean pis;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "consent_starter")
    private boolean consentStarter;

    @Column(name = "one_off_ais")
    private boolean oneOffAis;

    @Column(name = "risk_insights")
    private boolean riskInsights;

    public static Client from(ClientEvent event) {
        return new Client(event.getClientId(),
                event.getClientGroupId(),
                event.getName(),
                event.getClientUsersKyc().isPrivateIndividuals(),
                event.getClientUsersKyc().isEntities(),
                event.getDataEnrichment().isMerchantRecognition(),
                event.getDataEnrichment().isCategorization(),
                event.getDataEnrichment().isCycleDetection(),
                event.getDataEnrichment().isLabels(),
                event.isCam(),
                event.isPsd2Licensed(),
                event.isAis(),
                event.isPis(),
                false,
                event.isConsentStarter(),
                event.isOneOffAis(),
                event.isRiskInsights());
    }
}

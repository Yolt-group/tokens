package nl.ing.lovebird.tokens.clients.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    @NotNull private final ClientGroupDTO group;
    @NotNull private final UUID clientId;
    @NotEmpty private String name;

    private Boolean clientUsersKycPrivateIndividuals;
    private Boolean clientUsersKycEntities;
    private Boolean cam;
    private Boolean psd2Licensed;
    private Boolean ais;
    private Boolean pis;
    private Boolean dataEnrichmentMerchantRecognition;
    private Boolean dataEnrichmentCategorization;
    private Boolean dataEnrichmentCycleDetection;
    private Boolean dataEnrichmentLabels;
    private Boolean deleted;
    private Boolean consentStarter;
    private Boolean oneOffAis;
    private Boolean riskInsights;
}

package nl.ing.lovebird.tokens.clients.events;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Value
public class ClientEvent {
    @NotNull
    Action action;
    @NotNull
    UUID clientId;
    @NotNull
    UUID clientGroupId;
    @NotNull
    String name;
    boolean cam;
    boolean psd2Licensed;
    boolean ais;
    boolean pis;
    @NotNull
    ClientUsersKyc clientUsersKyc;
    @NotNull
    DataEnrichment dataEnrichment;
    boolean consentStarter;
    boolean oneOffAis;
    boolean riskInsights;

    @Value
    public static class ClientUsersKyc {
        boolean privateIndividuals;
        boolean entities;
    }

    @Value
    public static class DataEnrichment {
        boolean merchantRecognition;
        boolean categorization;
        boolean cycleDetection;
        boolean labels;
    }

    public enum Action {
        ADD, UPDATE, SYNC
    }
}

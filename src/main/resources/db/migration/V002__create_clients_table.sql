create table if not exists client(
    client_group_id uuid not null,
    client_id uuid not null constraint pk_clients primary key,

    name text not null,
    deleted boolean not null,

    psd2_licensed boolean not null,
    ais boolean not null,
    one_off_ais boolean not null,
    pis boolean not null,
    consent_starter boolean not null,

    kyc_private_individuals boolean not null,
    kyc_entities boolean not null,
    cam boolean not null,

    data_enrichment_merchant_recognition boolean not null,
    data_enrichment_categorization boolean not null,
    data_enrichment_cycle_detection boolean not null,
    data_enrichment_labels boolean not null,
    risk_insights boolean not null,

    FOREIGN KEY (client_group_id) REFERENCES client_group (id)
);
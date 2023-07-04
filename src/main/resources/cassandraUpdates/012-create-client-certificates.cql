CREATE TABLE IF NOT EXISTS client_proxy_client_certificates (
    client_id uuid,
    serialnumber text,
    certificate text,
    valid_to timestamp,
    subject text,
    csr text,
    created timestamp,
    PRIMARY KEY ((client_id), serialnumber)
);

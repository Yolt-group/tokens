CREATE TABLE IF NOT EXISTS client_verification_key (
    client_id uuid,
    key_id text,
    verification_key text,
    PRIMARY KEY ((client_id), key_id)
);

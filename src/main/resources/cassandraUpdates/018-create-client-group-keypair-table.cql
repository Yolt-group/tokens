CREATE TABLE IF NOT EXISTS client_group_keypair
(
  client_group_id             uuid,
  key_id                      text,
  provider_key                text,
  name                        text,
  service_types               set<text>,
  key_type                    text,
  key_algorithm               text,
  signature_algorithm         text,
  distinguished_names         map<text, text>,
  certificate_signing_request text,
  signed_certificate          text,
  PRIMARY KEY ((client_group_id), key_id, provider_key)
);
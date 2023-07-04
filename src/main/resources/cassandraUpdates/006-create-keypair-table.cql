CREATE TABLE IF NOT EXISTS client_provider_keypair
(
  client_id                   uuid,
  provider_key                text,
  key_id                      text,
  name                        text,
  service_types               set<text>,
  key_type                    text,
  key_algorithm               text,
  signature_algorithm         text,
  distinguished_names         map<text, text>,
  certificate_signing_request text,
  signed_certificate          text,
  PRIMARY KEY (client_id, provider_key, key_id)
);
# Tokens

Inter service authentication using JWTs.

## Run the application locally
In order to connect to cassandra/kafka you (unfortunately) need to change the JVM truststore that includes
the server certificate of cassandra/kafka:
```
-Djavax.net.ssl.trustStore=<path>\yolt-dev-trust-store 
-Djavax.net.ssl.trustStorePassword=xxx
```
See https://git.yolt.io/backend-tools/developer-toolbox


If you also want to connect to S3 from you local machine you have to do 2 additional things: 
1) You have to import amazon_s3_servercert.pem as well in that truststore. (it might be out of date, in that case just fetch it from the s3 api domain of amazon)
2) You have to 'impersonate' an instance of `tokens` (this webservice) on a given team environment. You need to copy a JWT from your team environment and put it in a file and reference to it in yolt.vault.auth.service-account-token-file.  This jwt can be retrieved from the 'tokens-token-xxx' secret, or exec into a pod and look at the serviceaccount that's injected by k8s.


## Context diagram
Architecture diagram draft:
[source](doc/architecture.puml)

Be aware of the Tokens structure:
[source](doc/understanding-tokens.puml)

How Tokens are created:
[source](doc/tokens-sequence.puml)



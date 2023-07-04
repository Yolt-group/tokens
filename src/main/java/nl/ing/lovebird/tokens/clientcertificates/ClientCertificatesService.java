package nl.ing.lovebird.tokens.clientcertificates;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.tokens.exception.RequestClientCertificateException;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.vault.client.VaultResponses;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Certificate;
import org.springframework.vault.support.VaultSignCertificateRequestResponse;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Service
public class ClientCertificatesService {
    private final ClientProxyClientCertificateRepository repository;
    private final String environment;
    private final Supplier<VaultTemplate> vaultTemplateSupplier;
    private final RestTemplate restTemplate;

    public ClientCertificatesService(ClientProxyClientCertificateRepository repository,
                                     @Value("${environment}") String environment,
                                     Supplier<VaultTemplate> vaultTemplateSupplier,
                                     RestTemplateBuilder restTemplateBuilder,
                                     @Value("${service.clients.url}") String clientsUrl) {
        this.repository = repository;
        this.environment = environment;
        this.vaultTemplateSupplier = vaultTemplateSupplier;
        this.restTemplate = restTemplateBuilder.rootUri(clientsUrl).build();
    }

    public List<ClientCertificateDTO> getClientCertificates(UUID clientId) {
        return repository.getClientCertificates(clientId)
                .stream()
                .map(clientProxyClientCertificate -> new ClientCertificateDTO(
                        clientProxyClientCertificate.getSerialNumber(),
                        clientProxyClientCertificate.getCertificate(),
                        clientProxyClientCertificate.getSubject(),
                        clientProxyClientCertificate.getValidTo(),
                        clientProxyClientCertificate.getCreated()))
                .collect(Collectors.toList());
    }

    /**
     * Request Vault to sign the CSR and retrieve a client certificate that can be used with Client-Proxy.
     * We use the sign-verbatim endpoint from Vault, to take the information from the CSR into the certificate. When
     * using the sign endpoint, Vault will override most of CSRs data with defaults.
     *
     * @param clientToken the client-id
     * @param csr         the Certificate Signing Request, Vault needs to sign
     * @return the signed Certificate together with information about the certificate
     */
    public ClientCertificateDTO requestClientProxyClientCertificate(ClientToken clientToken, String csr) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("csr", csr);
            request.put("format", "der");

            VaultTemplate vaultTemplate = vaultTemplateSupplier.get();
            VaultSignCertificateRequestResponse response = vaultTemplate.doWithSession(restOperations -> {
                try {
                    // sign-verbatim is needed to keep the information from the CSR.
                    return restOperations.postForObject("{environment}/pki/ingress/client-proxy/sign-verbatim/client-proxy",
                            request, VaultSignCertificateRequestResponse.class, environment);
                } catch (HttpStatusCodeException e) {
                    throw VaultResponses.buildException(e);
                }
            });

            Certificate certificate = response.getData();

            String serialNumber = certificate.getSerialNumber();
            String pem = writeCertificate(certificate.getX509Certificate());

            sumbitCertificateTowardsClients(clientToken, pem);

            Instant validTo = certificate.getX509Certificate().getNotAfter().toInstant();
            String subject = certificate.getX509Certificate().getSubjectDN().toString();
            Instant created = Instant.now();

            repository.save(new ClientProxyClientCertificate(clientToken.getClientIdClaim(), serialNumber, pem, subject, csr, validTo, created));

            return new ClientCertificateDTO(serialNumber, pem, subject, validTo, created);
        } catch (Exception e) {
            throw new RequestClientCertificateException("Request for client certificate failed", e);
        }
    }

    private void sumbitCertificateTowardsClients(ClientToken clientToken, String pem) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized());
        HttpEntity<NewClientMTLSCertificateDTO> httpRequest = new HttpEntity<>(new NewClientMTLSCertificateDTO(pem), headers);
        restTemplate.postForEntity("/internal/clients/{clientId}/client-mtls-certificates", httpRequest, ClientCertificateDTO.class, clientToken.getClientIdClaim());
    }

    private String writeCertificate(X509Certificate certificate) throws IOException {
        StringWriter output = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(output);
        pemWriter.writeObject(certificate);
        pemWriter.close();
        return output.toString();
    }

    @lombok.Value
    private static class NewClientMTLSCertificateDTO {
        String certificateChain;
    }
}

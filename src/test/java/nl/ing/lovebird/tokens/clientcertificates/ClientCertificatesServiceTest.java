package nl.ing.lovebird.tokens.clientcertificates;

import nl.ing.lovebird.clienttokens.ClientToken;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Certificate;
import org.springframework.vault.support.VaultSignCertificateRequestResponse;
import org.springframework.web.client.RestTemplate;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClientCertificatesServiceTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private ClientCertificatesService service;

    @Mock
    private ClientProxyClientCertificateRepository repository;

    @Mock
    private VaultTemplate vaultTemplate;
    @Mock
    private RestTemplate restTemplate;

    @Before
    public void setup() {
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        String clientsUrl = "https://clients/clients";
        when(restTemplateBuilder.rootUri(clientsUrl)).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service = new ClientCertificatesService(repository, "test", () -> vaultTemplate, restTemplateBuilder, clientsUrl);
    }

    @Test
    public void getClientCertificates() {
        // given
        UUID clientId = UUID.randomUUID();
        ClientProxyClientCertificate clientProxyClientCertificate1 = new ClientProxyClientCertificate(clientId, "serialnumber", "certificate", "subject", "csr", Instant.MAX, Instant.EPOCH);

        // when
        when(repository.getClientCertificates(clientId)).thenReturn(Collections.singletonList(clientProxyClientCertificate1));
        List<ClientCertificateDTO> clientCertificates = service.getClientCertificates(clientId);

        // then
        ClientCertificateDTO clientCertificate1 = new ClientCertificateDTO("serialnumber", "certificate", "subject", Instant.MAX, Instant.EPOCH);
        assertThat(clientCertificates, containsInAnyOrder(clientCertificate1));
    }

    @Test
    public void requestClientProxyClientCertificate() throws Exception {
        // given
        ClientToken clientToken = mock(ClientToken.class);
        String csr = "csr";
        VaultSignCertificateRequestResponse vaultResponse = new VaultSignCertificateRequestResponse();
        String certificate = generateSelfSignedX509Certificate();
        String serialNumber = "1e:d2:a3:53:54:39:2e:a2:0d:fe:54:55:10:f4:8c:d1:92:31:c8:e2";
        vaultResponse.setData(Certificate.of(serialNumber, certificate, "test"));

        // when
        when(vaultTemplate.doWithSession(any())).thenReturn(vaultResponse);
        ClientCertificateDTO certificateDTO = service.requestClientProxyClientCertificate(clientToken, csr);

        // then
        assertThat(certificateDTO, notNullValue());
        assertThat(certificateDTO.getSubject(), is("CN=John Doe"));
        assertThat(certificateDTO.getSerialnumber(), is(serialNumber));
    }

    private String generateSelfSignedX509Certificate() throws Exception {
        // yesterday
        Date validityBeginDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        // in 2 years
        Date validityEndDate = new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000);

        // GENERATE THE PUBLIC/PRIVATE RSA KEY PAIR
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024, new SecureRandom());

        java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // GENERATE THE X509 CERTIFICATE
        X500Principal dnName = new X500Principal("CN=John Doe");
        JcaX509v1CertificateBuilder certGen = new JcaX509v1CertificateBuilder(dnName, BigInteger.valueOf(System.currentTimeMillis()), validityBeginDate, validityEndDate, dnName, keyPair.getPublic());
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        ContentSigner contentSigner =
                new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                        .build(PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded()));

        X509CertificateHolder holder = certGen.build(contentSigner);

        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        return Base64.toBase64String(cert.getEncoded());
    }
}
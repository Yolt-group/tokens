package nl.ing.lovebird.tokens.clientcertificates;

import lombok.Value;

import java.time.Instant;

@Value
public class ClientCertificateDTO {
    private String serialnumber;
    private String certificate;
    private String subject;
    private Instant validTo;
    private Instant created;
}

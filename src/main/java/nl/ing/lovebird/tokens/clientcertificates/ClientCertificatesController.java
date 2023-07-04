package nl.ing.lovebird.tokens.clientcertificates;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.validation.bc.PEM;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "clients")
@RequestMapping("/clients/{clientId}/client-certificates")
class ClientCertificatesController {

    private final ClientCertificatesService clientCertificatesService;

    @Operation(summary = "Get all client certificates for a client",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully obtained list of client certificates")
            })
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<ClientCertificateDTO> listClientCertificates(@PathVariable final UUID clientId) {
        log.info("Getting list of client certificates for client with id {}", clientId);
        return clientCertificatesService.getClientCertificates(clientId);
    }

    @Operation(summary = "Get all client certificates for a client",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully created signed client certificate"),
                    @ApiResponse(responseCode = "400", description = "Provided input is incorrect")
            })
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public ClientCertificateDTO requestClientCertificate(
            @VerifiedClientToken ClientToken clientToken,
            @PathVariable final UUID clientId,
            @PEM(expectedTypes = PKCS10CertificationRequest.class) @RequestBody final String csr) {
        log.info("About to request a client certificate for client with id {}", clientId);
        return clientCertificatesService.requestClientProxyClientCertificate(clientToken, csr);
    }
}

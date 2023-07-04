package nl.ing.lovebird.tokens.configuration;

import nl.ing.lovebird.errorhandling.ErrorInfo;

public enum ErrorConstants implements ErrorInfo {

    CREATE_ACCESS_TOKEN_FAILED("002", "Server error while creating access token"),
    INVALID_PUBLIC_KEY("003", "Invalid public key"),
    INVALID_GRANT_TYPE("004", "Invalid grant type"),
    INVALID_SIGNATURE("005", "Signature invalid"),
    NO_CLIENT_UUID("006", "No client uuid found"),
    INVALID_REQUEST_TOKEN("007", "Invalid request token"),
    EMPTY_JWT_ID("008", "Empty JWT ID (jti)"),
    EMPTY_JWT_ISSUER("009", "Empty JWT Issued at (iss)"),
    EMPTY_JWT_ISSUED_AT("010", "Empty JWT Issued at (iat)"),
    INVALID_JWT_ID("011", "Invalid JWT Id format (jti)"),
    INVALID_ISSUED_AT("012", "Invalid JWT Issued at (iat)"),
    INVALID_CONTENT_TYPE("013", "Unsupported Content-Type"),
    INVALID_REQUEST_PARAMETERS("014", "Invalid request parameters"),
    VERIFICATION_KEY_ALREADY_EXISTS("015", "Verification key already exists"),
    NO_VERIFICATION_KEY("016", "No verification key found"),
    KEYPAIR_NAME_COLLISION("017", "Name is already used for another keypair"),
    INVALID_CERTIFICATE("018", "Invalid certificate"),
    CLIENT_NOT_FOUND("019", "Client not found by given id"),
    CLIENTGROUP_NOT_FOUND("020", "ClientGroup not found by given id"),
    CLIENT_EXISTS("021", "Client already exists"),
    CLIENTGROUP_EXISTS("022", "ClientGroup already exists"),
    KEYPAIR_NOT_FOUND("023", "Keypair not found");

    private final String code;
    private final String message;

    ErrorConstants(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

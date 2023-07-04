package nl.ing.lovebird.tokens.accesstokens;

import lombok.Getter;


@Getter
enum Scope {
    //TODO no defined scopes yet. camelcase-thingy because that is in the oAuth specs.
//    ACCOUNTS("accounts"),
//    TRANSACTIONS("transactions"),
//    AIS("ais"),
//    PIS("pis");
    ;
    private final String camelCaseName;

    Scope(String camelCaseName) {
        this.camelCaseName = camelCaseName;
    }
}

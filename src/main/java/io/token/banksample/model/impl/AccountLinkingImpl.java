package io.token.banksample.model.impl;

import io.token.banksample.model.AccessTokenAuthorization;
import io.token.banksample.model.AccountLinking;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccountAuthorizer;

import java.util.Map;

public class AccountLinkingImpl implements AccountLinking {
    private final BankAccountAuthorizer authorizer;
    private final Map<String, AccessTokenAuthorization> authorizations;

    public AccountLinkingImpl(
            BankAccountAuthorizer authorizer,
            Map<String, AccessTokenAuthorization> authorizations) {
        this.authorizer = authorizer;
        this.authorizations = authorizations;
    }

    @Override
    public BankAuthorization getBankAuthorization(String accessToken) {
        AccessTokenAuthorization authorization = authorizations.get(accessToken);
        return authorizer.createAuthorization(authorization.memberId(), authorization.accounts());
    }
}

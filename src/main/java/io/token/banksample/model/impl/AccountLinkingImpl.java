package io.token.banksample.model.impl;

import io.token.banksample.model.AccountLinking;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccountAuthorizer;

public class AccountLinkingImpl implements AccountLinking {
    private final BankAccountAuthorizer authorizer;

    public AccountLinkingImpl(BankAccountAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    @Override
    public BankAuthorization getBankAuthorization(String accessToken) {
        return authorizer.createAuthorization(); // TODO(Luke) add member id and accounts to config
    }
}

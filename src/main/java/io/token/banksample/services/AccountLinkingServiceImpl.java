package io.token.banksample.services;

import io.token.banksample.model.AccountLinking;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccountAuthorizer;
import io.token.sdk.api.service.AccountLinkingService;
import io.token.security.SecretKeyStore;
import io.token.security.TrustedKeyStore;
import io.token.security.keystore.KeyStoreFactory;

import java.util.Arrays;

public class AccountLinkingServiceImpl implements AccountLinkingService {
    private final AccountLinking accountLinking;

    public AccountLinkingServiceImpl(AccountLinking accountLinking) {
        this.accountLinking = accountLinking;
    }

    @Override
    public BankAuthorization getBankAuthorization(String accessToken) {
        return accountLinking.getBankAuthorization(accessToken);
    }
}

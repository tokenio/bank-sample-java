package io.token.banksample.model.impl;

import io.token.banksample.config.Account;
import io.token.banksample.model.Accounts;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.Collection;
import java.util.Optional;

/**
 * Configuration based account service implementation.
 */
public class AccountsImpl implements Accounts {
    private final Collection<Account> accounts;

    public AccountsImpl(Collection<Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<Balance> lookupBalance(BankAccount account) {
        return toSwiftAccount(account)
                .flatMap(swift -> accounts.stream()
                        .filter(a -> a.getBic().equals(swift.getBic()))
                        .filter(a -> a.getNumber().equals(swift.getAccount()))
                        .map(Account::getBalance)
                        .findFirst());
    }

    private static Optional<BankAccount.Swift> toSwiftAccount(BankAccount account) {
        if (account.getAccountCase() != BankAccount.AccountCase.SWIFT) {
            return Optional.empty();
        } else {
            return Optional.of(account.getSwift());
        }
    }
}

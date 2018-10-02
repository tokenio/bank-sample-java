package io.token.banksample.model.impl;

import static io.token.proto.bankapi.Bankapi.StatusCode.FAILURE_ACCOUNT_NOT_FOUND;
import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.SWIFT;
import static java.util.stream.Collectors.toMap;

import io.token.banksample.config.AccountConfig;
import io.token.banksample.model.Accounts;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.BankException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration based {@link Accounts} implementation.
 */
public class AccountsImpl implements Accounts {
    private final Map<String, AccountConfig> holdAccounts;
    private final Map<String, AccountConfig> fxAccounts;
    private final List<AccountConfig> accounts;

    public AccountsImpl(
            Collection<AccountConfig> holdAccounts,
            Collection<AccountConfig> fxAccounts,
            Collection<AccountConfig> customerAccounts) {
        this.holdAccounts = indexAccounts(holdAccounts);
        this.fxAccounts = indexAccounts(fxAccounts);
        this.accounts = new ArrayList<AccountConfig>() {{
            addAll(holdAccounts);
            addAll(fxAccounts);
            addAll(customerAccounts);
        }};
    }

    @Override
    public BankAccount getHoldAccount(String currency) {
        return Optional
                .ofNullable(holdAccounts.get(currency))
                .map(AccountConfig::toBankAccount)
                .orElseThrow(() -> new BankException(
                        FAILURE_ACCOUNT_NOT_FOUND,
                        "Hold account is not found for: " + currency));
    }

    @Override
    public BankAccount getFxAccount(String currency) {
        return Optional
                .ofNullable(fxAccounts.get(currency))
                .map(AccountConfig::toBankAccount)
                .orElseThrow(() -> new BankException(
                        FAILURE_ACCOUNT_NOT_FOUND,
                        "FX account is not found for: " + currency));
    }

    @Override
    public Collection<AccountConfig> getAllAccounts() {
        return accounts;
    }

    @Override
    public Optional<AccountConfig> tryLookupAccount(BankAccount account) {
        return toSwiftAccount(account)
                .flatMap(swift -> accounts.stream()
                        .filter(a -> a.getBic().equals(swift.getBic()))
                        .filter(a -> a.getNumber().equals(swift.getAccount()))
                        .findFirst());
    }

    private static Map<String, AccountConfig> indexAccounts(
            Collection<AccountConfig> accounts) {
        return accounts
                .stream()
                .collect(toMap(
                        a -> a.getBalance().getCurrency(),
                        a -> a));
    }

    private static Optional<BankAccount.Swift> toSwiftAccount(BankAccount account) {
        if (account.getAccountCase() != SWIFT) {
            return Optional.empty();
        } else {
            return Optional.of(account.getSwift());
        }
    }
}

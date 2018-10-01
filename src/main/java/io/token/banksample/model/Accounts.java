package io.token.banksample.model;

import static io.token.proto.bankapi.Bankapi.StatusCode.FAILURE_ACCOUNT_NOT_FOUND;

import io.token.banksample.config.AccountConfig;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.BankException;

import java.util.Collection;
import java.util.Optional;

/**
 * Provides access to the configured list of accounts.
 */
public interface Accounts {
    /**
     * Returns hold account for the given currency.
     *
     * @param currency currency to lookup the account for
     * @return looked up account
     */
    BankAccount getHoldAccount(String currency);

    /**
     * Returns FX account for the given currency.
     *
     * @param currency currency to lookup the account for
     * @return looked up account
     */
    BankAccount getFxAccount(String currency);

    /**
     * Returns all the configured accounts.
     *
     * @return all accounts
     */
    Collection<AccountConfig> getAllAccounts();

    /**
     * Looks up the account.
     *
     * @param account account to look up
     * @return looked up account
     */
    Optional<AccountConfig> tryLookupAccount(BankAccount account);

    /**
     * Looks up the account.
     *
     * @param account account to look up
     * @return looked up account
     */
    default AccountConfig lookupAccount(BankAccount account) {
        return this
                .tryLookupAccount(account)
                .orElseThrow(() -> new BankException(
                        FAILURE_ACCOUNT_NOT_FOUND,
                        "Account not found"));
    }
}

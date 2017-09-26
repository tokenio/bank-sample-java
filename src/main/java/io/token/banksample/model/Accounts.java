package io.token.banksample.model;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND;

import io.token.banksample.config.AccountConfig;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.PrepareTransferException;

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
     * Returns settlement account for the given currency.
     *
     * @param currency currency to lookup the account for
     * @return looked up account
     */
    BankAccount getSettlementAccount(String currency);

    /**
     * Returns FX account for the given currency.
     *
     * @param currency currency to lookup the account for
     * @return looked up account
     */
    BankAccount getFxAccount(String currency);

    /**
     * Returns reject account for the given currency. Used for testing, a
     * transfer initiated against a reject account is rejected
     *
     * @param currency currency to lookup the account for
     * @return looked up account
     */
    BankAccount getRejectAccount(String currency);

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
        // TODO: Fix the error code.
        return this
                .tryLookupAccount(account)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Account not found"));
    }
}

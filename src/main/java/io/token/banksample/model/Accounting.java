package io.token.banksample.model;

import io.token.banksample.config.AccountConfig;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.List;
import java.util.Optional;

/**
 * AccountTransaction accounting service. Abstracts away bank account data store.
 */
public interface Accounting {
    /**
     * Looks up account information.
     *
     * @param bankAccount account to lookup the info for
     * @return account info
     */
    Optional<AccountConfig> lookupAccount(BankAccount bankAccount);

    /**
     * Creates a new transaction.
     *
     * @param transaction new transaction
     */
    void createDebitTransaction(AccountTransaction transaction);
}

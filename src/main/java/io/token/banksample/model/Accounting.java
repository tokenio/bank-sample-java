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
     * Looks up account balance.
     *
     * @param account account to lookup the balance for
     * @return account balance if found
     */
    Optional<Balance> lookupBalance(BankAccount account);

    /**
     * Creates a new transaction.
     *
     * @param transaction new transaction
     */
    void createDebitTransaction(AccountTransaction transaction);

    /**
     * Looks up transaction given the account and transaction ID.
     *
     * @param account account to lookup the transaction for
     * @param transactionId transaction id
     * @return looked up transaction if found
     */
    Optional<AccountTransaction> lookupTransaction(BankAccount account, String transactionId);

    /**
     * Looks up transactions for the given account.
     *
     * @param account account to lookup the transactions for
     * @param offset the result offset
     * @param limit the limit on the number of results returned
     * @return list of looked up transactions
     */
    List<AccountTransaction> lookupTransactions(BankAccount account, int offset, int limit);
}

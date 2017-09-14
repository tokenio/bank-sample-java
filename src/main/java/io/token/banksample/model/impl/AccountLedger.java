package io.token.banksample.model.impl;

import io.token.banksample.model.AccountTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maintains per account ledger of transactions.
 */
final class AccountLedger {
    private final List<AccountTransaction> txs;
    private final Map<String, AccountTransaction> txById;

    AccountLedger() {
        this.txs = new ArrayList<>();
        this.txById = new HashMap<>();
    }

    /**
     * Adds transaction to the account ledger.
     *
     * @param transaction transaction to add
     */
    void addTransaction(AccountTransaction transaction) {
        txs.add(transaction);
        txById.put(transaction.getTransactionId(), transaction);
    }

    /**
     * Looks up a transaction by ID.
     *
     * @param id transaction ID
     * @return looked up transaction
     */
    Optional<AccountTransaction> lookupTransaction(String id) {
        return Optional.ofNullable(txById.get(id));
    }

    /**
     * Looks up multiple transactions.
     *
     * @param offset offset to start from
     * @param limit max number of transactions to lookup
     * @return list of transactions
     */
    List<AccountTransaction> lookupTransactions(int offset, int limit) {
        return txs.subList(offset, offset + limit);
    }
}

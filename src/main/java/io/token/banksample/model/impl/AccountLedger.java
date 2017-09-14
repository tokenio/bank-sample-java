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

    void addTransaction(AccountTransaction transaction) {
        txs.add(transaction);
        txById.put(transaction.getTransactionId(), transaction);
    }

    Optional<AccountTransaction> lookupTransaction(String id) {
        return Optional.ofNullable(txById.get(id));
    }

    List<AccountTransaction> lookupTransactions(int offset, int limit) {
        return txs.subList(offset, offset + limit);
    }
}

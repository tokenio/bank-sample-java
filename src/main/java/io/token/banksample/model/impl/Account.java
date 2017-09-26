package io.token.banksample.model.impl;

import static java.lang.Math.min;

import io.token.banksample.model.AccountTransaction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maintains a list of per account transactions.
 */
public class Account {
    private final List<AccountTransaction> transactions;
    private final Map<String, AccountTransaction> transactionsById;

    Account() {
        this.transactions = new LinkedList<>();
        this.transactionsById = new HashMap<>();
    }

    /**
     * Adds new transaction to the account.
     *
     * @param transaction transaction to add
     */
    void createTransaction(AccountTransaction transaction) {
        transactions.add(0, transaction);
        transactionsById.put(transaction.getId(), transaction);
    }

    /**
     * Looks up a payment by ID.
     *
     * @param id payment ID
     * @return looked up payment
     */
    Optional<AccountTransaction> lookupTransaction(String id) {
        return Optional.ofNullable(transactionsById.get(id));
    }

    /**
     * Looks up multiple payments.
     *
     * @param offset offset to start from
     * @param limit max number of payments to lookup
     * @return list of payments
     */
    List<AccountTransaction> lookupTransactions(int offset, int limit) {
        return transactions.subList(offset, min(offset + limit, transactions.size()));
    }
}

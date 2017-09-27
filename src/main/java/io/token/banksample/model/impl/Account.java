package io.token.banksample.model.impl;

import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_CANCELED;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static java.lang.Math.min;
import static java.math.BigDecimal.ROUND_FLOOR;

import io.token.banksample.model.AccountTransaction;
import io.token.sdk.api.Balance;
import io.token.sdk.api.TransferException;

import java.math.BigDecimal;
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
    private final String currency;
    private double balanceAvailable;
    private double balanceCurrent;

    Account(String currency, double balanceAvailable, double balanceCurrent) {
        this.currency = currency;
        this.transactions = new LinkedList<>();
        this.transactionsById = new HashMap<>();
        this.balanceAvailable = balanceAvailable;
        this.balanceCurrent = balanceCurrent;
    }

    Balance getBalance() {
        return Balance.create(
                currency,
                BigDecimal.valueOf(balanceAvailable).setScale(2, ROUND_FLOOR),
                BigDecimal.valueOf(balanceCurrent).setScale(2, ROUND_FLOOR));
    }

    /**
     * Adds new transaction to the account.
     *
     * @param transaction transaction to add
     */
    void createTransaction(AccountTransaction transaction) {
        if (transaction.getAmount() > balanceAvailable) {
            throw new TransferException(FAILURE_INSUFFICIENT_FUNDS, "Balance exceeded");
        }
        transactions.add(0, transaction);
        transactionsById.put(transaction.getId(), transaction);
        balanceAvailable -= transaction.getAmount();
    }

    /**
     * Adds new transaction to the account.
     *
     * @param transactionId ID of the transaction to commit
     */
    Optional<AccountTransaction> commitTransaction(String transactionId) {
        return Optional
                .ofNullable(transactionsById.get(transactionId))
                .map(t -> {
                    balanceCurrent -= t.getAmount();
                    t.setStatus(SUCCESS);
                    return t;
                });
    }

    /**
     * Adds new transaction to the account.
     *
     * @param transactionId ID of the transaction to commit
     */
    Optional<AccountTransaction> rollbackTransaction(String transactionId) {
        return Optional
                .ofNullable(transactionsById.get(transactionId))
                .map(t -> {
                    balanceAvailable += t.getAmount();
                    t.setStatus(FAILURE_CANCELED);
                    return t;
                });
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

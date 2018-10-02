package io.token.banksample.model.impl;

import static io.token.proto.bankapi.Bankapi.StatusCode.FAILURE_CANCELED;
import static io.token.proto.bankapi.Bankapi.StatusCode.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.bankapi.Bankapi.StatusCode.SUCCESS;
import static java.lang.Math.min;
import static java.math.BigDecimal.ROUND_FLOOR;
import static java.util.Collections.emptyList;

import io.token.banksample.model.AccountTransaction;
import io.token.sdk.api.Balance;
import io.token.sdk.api.TransferException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maintains a list of per account transactions.
 */
class Account {
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
                BigDecimal.valueOf(balanceCurrent).setScale(2, ROUND_FLOOR),
                Instant.now().toEpochMilli(),
                emptyList());
    }

    /**
     * Adds new transaction to the account.
     *
     * @param transaction transaction to add
     * @return true if transaction has been created, false if duplicate
     */
    boolean createTransaction(AccountTransaction transaction) {
        if (transactionsById.containsKey(transaction.getId())) {
            return false;
        }

        if (transaction.getAmount() > balanceAvailable) {
            throw new TransferException(FAILURE_INSUFFICIENT_FUNDS, "Balance exceeded");
        }

        transactions.add(0, transaction);
        transactionsById.put(transaction.getId(), transaction);
        balanceAvailable -= transaction.getAmount();
        return true;
    }

    /**
     * Commits a transaction. Note this method is not called by Token; the specifics of when a
     * transaction is considered complete is up to the bank and payment scheme used.
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
     * Cancels a transaction. Note this method is not called by Token; the specifics of when a
     * transaction is rejected is up to the bank and payment scheme used.
     *
     * @param transactionId ID of the transaction to cancel
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

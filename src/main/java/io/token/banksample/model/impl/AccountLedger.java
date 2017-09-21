package io.token.banksample.model.impl;

import static java.lang.Math.min;

import io.token.banksample.model.AccountTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maintains per account ledger of transactions.
 *
 * TODO: This is not a ledger anymore?
 * TODO: Do we still need transactions here?
 */
final class AccountLedger {
    private final List<AccountTransaction> payments;
    private final Map<String, AccountTransaction> paymentsById;
    private final List<AccountLedgerEntry> ledger;

    AccountLedger() {
        this.payments = new LinkedList<>();
        this.paymentsById = new HashMap<>();
        this.ledger = new ArrayList<>();
    }

    /**
     * Adds new transaction to the ledger.
     *
     * @param transaction transaction to add
     */
    void createPayment(AccountTransaction transaction) {
        payments.add(0, transaction);
        paymentsById.put(transaction.getId(), transaction);
    }

    /**
     * Deletes payment from the ledger.
     *
     * @param paymentId payment id
     */
    void deletePayment(String paymentId) {
        AccountTransaction deleted = paymentsById.remove(paymentId);
        payments.remove(deleted);
    }

    /**
     * Looks up a payment by ID.
     *
     * @param id payment ID
     * @return looked up payment
     */
    Optional<AccountTransaction> lookupPayment(String id) {
        return Optional.ofNullable(paymentsById.get(id));
    }

    /**
     * Looks up multiple payments.
     *
     * @param offset offset to start from
     * @param limit max number of payments to lookup
     * @return list of payments
     */
    List<AccountTransaction> lookupPayments(int offset, int limit) {
        return payments.subList(offset, min(offset + limit, payments.size()));
    }

    /**
     * Posts transaction to the ledger.
     *
     * @param transaction transaction to post
     */
    void post(AccountLedgerEntry transaction) {
        ledger.add(transaction);
    }
}

package io.token.banksample.model.impl;

import static java.lang.Math.min;

import io.token.banksample.model.Payment;

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
    private final List<Payment> payments;
    private final Map<String, Payment> paymentsById;

    AccountLedger() {
        this.payments = new LinkedList<>();
        this.paymentsById = new HashMap<>();
    }

    /**
     * Adds new payment to the ledger.
     *
     * @param payment payment to add
     */
    void createPayment(Payment payment) {
        payments.add(0, payment);
        paymentsById.put(payment.getId(), payment);
    }

    /**
     * Deletes payment from the ledger.
     *
     * @param paymentId payment id
     */
    void deletePayment(String paymentId) {
        Payment deleted = paymentsById.remove(paymentId);
        payments.remove(deleted);
    }

    /**
     * Looks up a payment by ID.
     *
     * @param id payment ID
     * @return looked up payment
     */
    Optional<Payment> lookupPayment(String id) {
        return Optional.ofNullable(paymentsById.get(id));
    }

    /**
     * Looks up multiple payments.
     *
     * @param offset offset to start from
     * @param limit max number of payments to lookup
     * @return list of payments
     */
    List<Payment> lookupPayments(int offset, int limit) {
        return payments.subList(offset, min(offset + limit, payments.size()));
    }
}

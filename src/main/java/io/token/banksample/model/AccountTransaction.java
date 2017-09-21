package io.token.banksample.model;

import com.google.auto.value.AutoValue;
import io.token.proto.common.account.AccountProtos.BankAccount;

/**
 * Represents a transaction posted to the source and destination accounts. The
 * change credits one account and debits the other.
 */
@AutoValue
public abstract class AccountTransaction {
    /**
     * Creates new debit transaction.
     *
     * @param paymentId payment id
     * @param transactionId transaction id
     * @param account account that transaction is posted for
     * @param counterPartyAccount counter party account
     * @param amount transaction amount
     * @param currency transaction currency
     * @return newly created transaction
     */
    static AccountTransaction debit(
            String paymentId,
            String transactionId,
            BankAccount account,
            BankAccount counterPartyAccount,
            double amount,
            String currency) {
        return new AutoValue_AccountTransaction(
                paymentId,
                transactionId,
                account,
                counterPartyAccount,
                - amount,
                currency);
    }

    /**
     * Creates new credit transaction.
     *
     * @param paymentId payment id
     * @param transactionId transaction id
     * @param account account that transaction is posted for
     * @param counterPartyAccount counter party account
     * @param amount transaction amount
     * @param currency transaction currency
     * @return newly created transaction
     */
    static AccountTransaction credit(
            String paymentId,
            String transactionId,
            BankAccount account,
            BankAccount counterPartyAccount,
            double amount,
            String currency) {
        return new AutoValue_AccountTransaction(
                paymentId,
                transactionId,
                account,
                counterPartyAccount,
                + amount,
                currency);
    }

    /**
     * Returns unique payment id.
     *
     * @return payment id
     */
    public abstract String getPaymentId();

    /**
     * Returns unique transaction id.
     *
     * @return transaction id
     */
    public abstract String getTransactionId();

    /**
     * Returns transaction account.
     *
     * @return transaction account
     */
    public abstract BankAccount getAccount();

    /**
     * Returns transaction counterparty account.
     *
     * @return transaction counterparty account
     */
    public abstract BankAccount getCounterPartyAccount();

    /**
     * Returns transaction amount.
     *
     * @return transaction amount
     */
    public abstract double getAmount();

    /**
     * Returns transaction currency.
     *
     * @return transaction currency
     */
    public abstract String getCurrency();
}

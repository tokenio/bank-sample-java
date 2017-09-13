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
     * @param transferId transfer id
     * @param transactionId transaction id
     * @param account account that transaction is posted for
     * @param counterPartyAccount counter party account
     * @param amount transaction amount
     * @return newly created transaction
     */
    static AccountTransaction debit(
            String transferId,
            String transactionId,
            BankAccount account,
            BankAccount counterPartyAccount,
            double amount) {
        return new AutoValue_AccountTransaction(
                transferId,
                transactionId,
                account,
                counterPartyAccount,
                - amount);
    }

    /**
     * Creates new credit transaction.
     *
     * @param transferId transfer id
     * @param transactionId transaction id
     * @param account account that transaction is posted for
     * @param counterPartyAccount counter party account
     * @param amount transaction amount
     * @return newly created transaction
     */
    static AccountTransaction credit(
            String transferId,
            String transactionId,
            BankAccount account,
            BankAccount counterPartyAccount,
            double amount) {
        return new AutoValue_AccountTransaction(
                transferId,
                transactionId,
                account,
                counterPartyAccount,
                + amount);
    }

    /**
     * Returns unique transfer id.
     *
     * @return transfer id
     */
    public abstract String getTransferId();

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
}

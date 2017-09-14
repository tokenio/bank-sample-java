package io.token.banksample.model;

import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.PROCESSING;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.CREDIT;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static java.lang.Math.abs;

import com.google.auto.value.AutoValue;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.transaction.TransactionProtos.Transaction;

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
     * @param currency transaction currency
     * @return newly created transaction
     */
    public static AccountTransaction debit(
            String transferId,
            String transactionId,
            BankAccount account,
            BankAccount counterPartyAccount,
            double amount,
            String currency) {
        return new AutoValue_AccountTransaction(
                transferId,
                transactionId,
                account,
                counterPartyAccount,
                - amount,
                currency);
    }

    /**
     * Creates new credit transaction.
     *
     * @param transferId transfer id
     * @param transactionId transaction id
     * @param account account that transaction is posted for
     * @param counterPartyAccount counter party account
     * @param amount transaction amount
     * @param currency transaction currency
     * @return newly created transaction
     */
    public static AccountTransaction credit(
            String transferId,
            String transactionId,
            BankAccount account,
            BankAccount counterPartyAccount,
            double amount,
            String currency) {
        return new AutoValue_AccountTransaction(
                transferId,
                transactionId,
                account,
                counterPartyAccount,
                + amount,
                currency);
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

    /**
     * Returns transaction currency.
     *
     * @return transaction currency
     */
    public abstract String getCurrency();

    /**
     * Converts this object to the transaction as defined by the integration
     * API.
     *
     * @return transaction
     */
    public Transaction toTransaction() {
        return Transaction.newBuilder()
                .setId(getTransactionId())
                .setTokenTransferId(getTransferId())
                .setType(getAmount() > 0 ? CREDIT : DEBIT)
                .setStatus(PROCESSING)
                .setAmount(MoneyProtos.Money.newBuilder()
                        .setValue(Double.toString(abs(getAmount())))
                        .setCurrency(getCurrency())
                        .build())
                .build();
    }
}

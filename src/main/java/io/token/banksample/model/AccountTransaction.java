package io.token.banksample.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;

/**
 * Represents an account transaction. The transaction captures from, to, amount
 * and the current status.
 */
public final class AccountTransaction {
    private final TransactionType type;
    private final String id;
    private final String referenceId;
    private final BankAccount from;
    private final BankAccount to;
    private final double amount;
    private final String currency;
    private final String description;
    private volatile TransactionStatus status;

    /**
     * Creates new transaction builder.
     *
     * @param type transaction type
     * @return transaction builder
     */
    public static Builder builder(TransactionType type) {
        return new Builder(type);
    }

    /**
     * Creates new transaction instance.
     *
     * @param type transaction type
     * @param id  transaction id
     * @param referenceId transaction reference id, used to capture caller
     *      transaction identifier
     * @param from from / remitter account
     * @param to to / beneficiary account
     * @param amount transaction amount
     * @param currency transaction currency
     * @param description transaction description
     */
    private AccountTransaction(
            TransactionType type,
            String id,
            String referenceId,
            BankAccount from,
            BankAccount to,
            double amount,
            String currency,
            String description) {
        this.type = type;
        this.id = id;
        this.referenceId = referenceId;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = TransactionStatus.PROCESSING;
    }

    /**
     * Returns transaction type.
     *
     * @return transaction type
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Returns transaction ID.
     *
     * @return transaction ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns transaction reference ID.
     *
     * @return transaction reference ID
     */
    public String getReferenceId() {
        return referenceId;
    }

    /**
     * Returns transaction from / remitter.
     *
     * @return transaction from / remitter
     */
    public BankAccount getFrom() {
        return from;
    }

    /**
     * Returns transaction to / beneficiary.
     *
     * @return transaction to / beneficiary
     */
    public BankAccount getTo() {
        return to;
    }

    /**
     * Returns transaction amount.
     *
     * @return transaction amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Returns transaction currency.
     *
     * @return transaction currency
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * Sets transaction status.
     *
     * @param status new transaction status
     */
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    /**
     * Converts this object to the transaction as defined by the integration
     * API.
     *
     * @return transaction
     */
    public Transaction toTransaction() {
        return Transaction.newBuilder()
                .setId(id)
                .setTokenTransferId(getReferenceId())
                .setType(getType())
                .setStatus(status)
                .setDescription(description)
                .setAmount(MoneyProtos.Money.newBuilder()
                        .setValue(Double.toString(getAmount()))
                        .setCurrency(getCurrency())
                        .build())
                .build();
    }

    /**
     * Used to build {@link AccountTransaction} instances.
     */
    public static final class Builder {
        private final TransactionType type;
        private String id;
        private String referenceId;
        private BankAccount from;
        private BankAccount to;
        private double amount;
        private String currency;
        private String description;

        /**
         * Creates new builder.
         *
         * @param type transaction type
         */
        private Builder(TransactionType type) {
            this.type = type;
            this.description = "";
        }

        /**
         * Sets unique transaction ID.
         *
         * @param id transaction ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets transaction reference id. Reference ID captures the external
         * transaction ID.
         *
         * @param referenceId reference id
         * @return this builder
         */
        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        /**
         * Sets from / remitter account.
         *
         * @param account remitter account
         * @return this builder
         */
        public Builder from(BankAccount account) {
            this.from = account;
            return this;
        }

        /**
         * Sets to / beneficiary account.
         *
         * @param account beneficiary account
         * @return this builder
         */
        public Builder to(BankAccount account) {
            this.to = account;
            return this;
        }

        /**
         * Sets transaction amount.
         *
         * @param amount transaction amount
         * @param currency transaction currency
         * @return this builder
         */
        public Builder amount(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
            return this;
        }

        /**
         * Sets transaction description.
         *
         * @param description transaction description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Finishes building {@link AccountTransaction} instance and returns it
         * to the caller.
         *
         * @return built transaction instance
         */
        public AccountTransaction build() {
            checkArgument(amount > 0, "Amount must be set");
            return new AccountTransaction(
                    type,
                    checkNotNull(id, "AccountTransaction id must be set"),
                    checkNotNull(referenceId, "AccountTransaction reference id must be set"),
                    checkNotNull(from, "'From' account must be set"),
                    checkNotNull(to, "'To' account must be set"),
                    amount,
                    checkNotNull(currency, "Currency must be set"),
                    description);
        }
    }
}

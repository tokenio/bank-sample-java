package io.token.banksample.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.transaction.TransactionProtos;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;

public final class AccountTransaction {
    private final TransactionType type;
    private final String id;
    private final String referenceId;
    private final BankAccount from;
    private final BankAccount to;
    private final double amount;
    private final String currency;
    private final String description;
    private TransactionStatus status;

    public static Builder builder(TransactionType type) {
        return new Builder(type);
    }

    private AccountTransaction(
            TransactionType type,
            String paymentId,
            String referenceId,
            BankAccount from,
            BankAccount to,
            double amount,
            String currency,
            String description) {
        this.type = type;
        this.id = paymentId;
        this.referenceId = referenceId;
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = TransactionStatus.PROCESSING;
    }

    public TransactionType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public BankAccount getFrom() {
        return from;
    }

    public BankAccount getTo() {
        return to;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    /**
     * Converts this object to the transaction as defined by the integration
     * API.
     *
     * @return transaction
     */
    public TransactionProtos.Transaction toTransaction() {
        return TransactionProtos.Transaction.newBuilder()
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

    public static final class Builder {
        private final TransactionType type;
        private String id;
        private String referenceId;
        private BankAccount from;
        private BankAccount to;
        private double amount;
        private String currency;
        private String description;

        private Builder(TransactionType type) {
            this.type = type;
            this.description = "";
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public Builder from(BankAccount account) {
            this.from = account;
            return this;
        }

        public Builder to(BankAccount account) {
            this.to = account;
            return this;
        }

        public Builder withAmount(double amount, String currency) {
            this.amount = amount;
            this.currency = currency;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

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

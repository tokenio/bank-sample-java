package io.token.banksample.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.money.MoneyProtos;
import io.token.proto.common.transaction.TransactionProtos;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;

import java.util.ArrayList;
import java.util.List;

public final class Payment {
    private final TransactionType type;
    private final String id;
    private final String referenceId;
    private final BankAccount from;
    private final BankAccount to;
    private final double amount;
    private final String currency;
    private final String description;
    private final List<AccountTransfer> transfers;
    private final List<AccountTransaction> transactions;
    private TransactionStatus status;

    public static Builder builder(TransactionType type) {
        return new Builder(type);
    }

    private Payment(
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
        this.transfers = new ArrayList<>();
        this.transactions = new ArrayList<>();
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

    public void addTransaction(AccountTransaction transaction) {
        transactions.add(transaction);
    }

    public void addTransfer(AccountTransfer transfer) {
        transfers.add(transfer);
        transactions.add(transfer.toTransactionPair().getDebit());
        transactions.add(transfer.toTransactionPair().getCredit());
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

        public Payment build() {
            checkArgument(amount > 0, "Amount must be set");
            return new Payment(
                    type,
                    checkNotNull(id, "Payment id must be set"),
                    checkNotNull(referenceId, "Payment reference id must be set"),
                    checkNotNull(from, "'From' account must be set"),
                    checkNotNull(to, "'To' account must be set"),
                    amount,
                    checkNotNull(currency, "Currency must be set"),
                    description);
        }
    }
}

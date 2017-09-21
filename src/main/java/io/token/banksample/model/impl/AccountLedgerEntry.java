package io.token.banksample.model.impl;

import com.google.auto.value.AutoValue;
import io.token.banksample.model.AccountTransfer;
import io.token.proto.common.account.AccountProtos.BankAccount;

/**
 * Represents a ledger entry posted to the source and destination accounts.
 * The change credits one account and debits the other.
 */
@AutoValue
public abstract class AccountLedgerEntry {
    /**
     * Creates new debit ledger entry.
     *
     * @param transfer to extract the transaction information from
     * @return newly created transaction
     */
    static AccountLedgerEntry debit(AccountTransfer transfer) {
        return new AutoValue_AccountLedgerEntry(
                transfer.getTransferId() + ":debit",
                    transfer.getTransferId(),
                    transfer.getFrom(),
                    transfer.getTo(),
                    - transfer.getAmount(),
                    transfer.getCurrency());
    }

    /**
     * Creates new credit ledger entry.
     *
     * @param transfer to extract the transaction information from
     * @return newly created transaction
     */
    static AccountLedgerEntry credit(AccountTransfer transfer) {
        return new AutoValue_AccountLedgerEntry(
                transfer.getTransferId() + ":credit",
                transfer.getTransferId(),
                transfer.getFrom(),
                transfer.getTo(),
                + transfer.getAmount(),
                transfer.getCurrency());
    }

    /**
     * Returns unique ledger entry id.
     *
     * @return ledger entry id
     */
    public abstract String getId();

    /**
     * Returns unique payment id that the entry ties to.
     *
     * @return payment id
     */
    public abstract String getPaymentId();

    /**
     * Returns account.
     *
     * @return transaction account
     */
    public abstract BankAccount getAccount();

    /**
     * Returns counterparty account.
     *
     * @return counterparty account
     */
    public abstract BankAccount getCounterPartyAccount();

    /**
     * Returns amount.
     *
     * @return amount
     */
    public abstract double getAmount();

    /**
     * Returns currency.
     *
     * @return currency
     */
    public abstract String getCurrency();
}

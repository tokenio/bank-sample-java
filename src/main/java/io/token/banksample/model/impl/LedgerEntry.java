package io.token.banksample.model.impl;

import com.google.auto.value.AutoValue;
import io.token.proto.common.account.AccountProtos.BankAccount;

/**
 * Represents an account journal entry posted to the source and
 * destination accounts. The change credits one account and debits
 * the other.
 */
@AutoValue
public abstract class LedgerEntry {
    /**
     * Creates new debit journal entry.
     *
     * @param transfer to extract the transaction information from
     * @return newly created transaction
     */
    static LedgerEntry debit(AccountTransfer transfer) {
        return new AutoValue_LedgerEntry(
                transfer.getTransferId() + ":debit",
                    transfer.getTransferId(),
                    transfer.getFrom(),
                    transfer.getTo(),
                    - transfer.getAmount(),
                    transfer.getCurrency());
    }

    /**
     * Creates new credit journal entry.
     *
     * @param transfer to extract the transaction information from
     * @return newly created transaction
     */
    static LedgerEntry credit(AccountTransfer transfer) {
        return new AutoValue_LedgerEntry(
                transfer.getTransferId() + ":credit",
                transfer.getTransferId(),
                transfer.getFrom(),
                transfer.getTo(),
                + transfer.getAmount(),
                transfer.getCurrency());
    }

    /**
     * Returns unique journal entry id.
     *
     * @return journal entry id
     */
    public abstract String getId();

    /**
     * Returns unique transaction id that the entry ties to.
     *
     * @return transaction id
     */
    public abstract String getTransactionId();

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

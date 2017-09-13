package io.token.banksample.model;

import com.google.auto.value.AutoValue;

/**
 * Represents a transaction posted to the source and destination accounts. The
 * change credits one account and debits the other.
 */
@AutoValue
public abstract class AccountTransactionPair {
    /**
     * Creates new transaction pair, debit + credit.
     *
     * @param debit debit transaction leg
     * @param credit credit transaction leg
     * @return transaction pair
     */
    static AccountTransactionPair transactionPair(
            AccountTransaction debit,
            AccountTransaction credit) {
        return new AutoValue_AccountTransactionPair(debit, credit);
    }

    /**
     * Returns debit transaction.
     *
     * @return debit transaction
     */
    public abstract AccountTransaction getDebit();

    /**
     * Returns credit transaction.
     *
     * @return credit transaction
     */
    public abstract AccountTransaction getCredit();
}

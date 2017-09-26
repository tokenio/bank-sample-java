package io.token.banksample.model.impl;

import static io.token.banksample.model.impl.LedgerEntry.credit;
import static io.token.banksample.model.impl.LedgerEntry.debit;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains ledger of transactions.
 */
final class AccountingLedger {
    private final List<LedgerEntry> ledger;

    AccountingLedger() {
        this.ledger = new ArrayList<>();
    }

    /**
     * Posts a transfer to ledger. Each transfer results in two transactions
     * posted.
     *
     * @param transfer account transfer
     */
    synchronized void post(AccountTransfer transfer) {
        post(singletonList(transfer));
    }

    /**
     * Posts transfers to ledger. Each transfer results in two transactions
     * posted.
     *
     * @param transfers account transfers
     */
    synchronized void post(AccountTransfer... transfers) {
        post(asList(transfers));
    }

    /**
     * Posts transfers to ledger. Each transfer results in two transactions
     * posted.
     *
     * @param transfers account transfers
     */
    private void post(List<AccountTransfer> transfers) {
        for (AccountTransfer transfer : transfers) {
            post(debit(transfer));
            post(credit(transfer));
        }
    }

    /**
     * Posts transaction to the ledger.
     *
     * @param transaction transaction to post
     */
    private void post(LedgerEntry transaction) {
        ledger.add(transaction);
    }
}

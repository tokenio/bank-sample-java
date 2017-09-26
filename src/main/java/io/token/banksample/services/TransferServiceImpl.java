package io.token.banksample.services;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_GENERIC;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;

import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.Accounting;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.sdk.api.Balance;
import io.token.sdk.api.PrepareTransferException;
import io.token.sdk.api.Transfer;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.TransferService;

/**
 * Sample implementation of the {@link TransferService}. Returns fake data.
 */
public class TransferServiceImpl implements TransferService {
    private final Accounting accounts;

    public TransferServiceImpl(Accounting accounts) {
        this.accounts = accounts;
    }

    @Override
    public Transaction transfer(Transfer transfer) throws TransferException {
        Balance balance = accounts
                .lookupBalance(transfer.getAccount())
                .orElseThrow(() -> new TransferException(
                        FAILURE_GENERIC,
                        "Account not found: " + transfer.getAccount()));

        if (balance.getAvailable().compareTo(transfer.getTransactionAmount()) < 0) {
            throw new PrepareTransferException(
                    FAILURE_INSUFFICIENT_FUNDS,
                    "Balance exceeded");
        }

        // TODO: Fail this if currency doesn't match the account.
        // And say that we don't support FX here.
        // TODO: Make this idempotent.
        AccountTransaction transaction = AccountTransaction.builder(DEBIT)
                .referenceId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(transfer.getDestinations().get(0).getAccount())
                .amount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .transferAmount(
                        transfer.getQuote(),
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .description(transfer.getDescription())
                .build();
        accounts.createDebitTransaction(transaction);

        // TODO: Not sure what would normally happen here.

        return transaction.toTransaction();
    }
}

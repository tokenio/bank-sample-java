package io.token.banksample.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_INSUFFICIENT_FUNDS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_GENERIC;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;

import io.token.banksample.model.Accounting;
import io.token.banksample.model.Payment;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.sdk.api.Balance;
import io.token.sdk.api.PrepareTransferException;
import io.token.sdk.api.Transfer;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.TransferService;

import java.util.Optional;
import java.util.UUID;

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
        Optional<Balance> balance = accounts.lookupBalance(transfer.getAccount());
        if (!balance.isPresent()) {
            throw new TransferException(
                    FAILURE_GENERIC,
                    "Account not found: " + transfer.getAccount());
        }

        if (balance.get().getAvailable().compareTo(transfer.getTransactionAmount()) < 0) {
            throw new PrepareTransferException(
                    FAILURE_INSUFFICIENT_FUNDS,
                    "Balance exceeded");
        }

        Payment payment = Payment.builder(DEBIT)
                .id(UUID.randomUUID().toString())
                .referenceId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(transfer.getDestinations().get(0).getAccount())
                .withAmount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .withDescription(transfer.getDescription())
                .build();
        accounts.createPayment(payment);
        return payment.toTransaction();
    }
}

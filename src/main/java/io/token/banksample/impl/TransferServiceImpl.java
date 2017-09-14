package io.token.banksample.impl;

import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.AccountTransactionPair;
import io.token.banksample.model.AccountTransfer;
import io.token.banksample.model.Accounting;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.sdk.api.Transfer;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.TransferService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample implementation of the {@link TransferService}. Returns fake data.
 */
public class TransferServiceImpl implements TransferService {
    private static final Logger logger = LoggerFactory.getLogger(TransferServiceImpl.class);
    private final Accounting accounts;

    public TransferServiceImpl(Accounting accounts) {
        this.accounts = accounts;
    }

    @Override
    public Transaction transfer(Transfer transfer) throws TransferException {
        AccountTransactionPair txs = accounts.transfer(AccountTransfer.transfer()
                .withId(transfer.getTokenTransferId())
                .from(transfer.getAccount())
                .to(accounts.getSettlementAccount(transfer.getTransactionAmountCurrency()))
                .withAmount(
                        transfer.getTransactionAmount().doubleValue(),
                        transfer.getTransactionAmountCurrency())
                .build());
        AccountTransaction debit = txs.getDebit();
        return debit.toTransaction();
    }
}

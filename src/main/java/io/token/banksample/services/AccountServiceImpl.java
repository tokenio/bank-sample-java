package io.token.banksample.services;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.banksample.model.Accounting;
import io.token.proto.PagedList;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.sdk.api.Balance;
import io.token.sdk.api.service.AccountService;

import java.util.List;
import java.util.Optional;

/**
 * Sample implementation of the {@link AccountService}. Returns fake data.
 */
public class AccountServiceImpl implements AccountService {
    private final Accounting accounts;

    public AccountServiceImpl(Accounting accounts) {
        this.accounts = accounts;
    }

    @Override
    public Balance getBalance(BankAccount account) {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }

    @Override
    public Optional<Transaction> getTransaction(BankAccount account, String transactionId) {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }

    @Override
    public PagedList<Transaction, String> getTransactions(
            BankAccount account,
            String cursor,
            int limit) {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }

    @Override
    public List<TransferEndpoint> resolveTransferDestination(BankAccount bankAccount) {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }
}

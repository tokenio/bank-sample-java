package io.token.banksample.impl;

import static io.grpc.Status.NOT_FOUND;
import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_DESTINATION_ACCOUNT_NOT_FOUND;
import static java.util.stream.Collectors.toList;

import io.token.banksample.config.Account;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Payment;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.CustomerData;
import io.token.sdk.api.Balance;
import io.token.sdk.api.PrepareTransferException;
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
        return accounts
                .lookupBalance(account)
                .orElseThrow(() -> NOT_FOUND
                        .withDescription("Account not found")
                        .asRuntimeException());
    }

    @Override
    public CustomerData getCustomerData(BankAccount bankAccount) {
        // TODO: PrepareTransferException is wrong here, need to either user
        // Status.NOT_FOUND or have a global list of errors that can be used
        // in multiple places.
        // Also makes no sense for this to return _DESTINATION_*_NOT_FOUND.
        // Could be the source account.
        Account account = accounts
                .lookupAccount(bankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_DESTINATION_ACCOUNT_NOT_FOUND,
                        "Account not found"));

        return CustomerData.newBuilder()
                .addLegalNames(account.getName())
                .setAddress(account.getAddress())
                .build();
    }

    @Override
    public Optional<Transaction> getTransaction(BankAccount account, String transactionId) {
        return accounts
                .lookupPayment(account, transactionId)
                .map(Payment::toTransaction);
    }

    @Override
    public List<Transaction> getTransactions(BankAccount account, int offset, int limit) {
        return accounts
                .lookupPayments(account, offset, limit)
                .stream()
                .map(Payment::toTransaction)
                .collect(toList());
    }
}

package io.token.banksample.services;

import static io.token.proto.bankapi.Bankapi.StatusCode.FAILURE_ACCOUNT_NOT_FOUND;
import static java.util.stream.Collectors.toList;

import io.token.banksample.config.AccountConfig;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.Accounting;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.Sepa;
import io.token.proto.common.account.AccountProtos.BankAccount.Swift;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.CustomerData;
import io.token.sdk.api.Balance;
import io.token.sdk.api.BankException;
import io.token.sdk.api.service.AccountService;

import java.util.ArrayList;
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
                .orElseThrow(() -> new BankException(
                        FAILURE_ACCOUNT_NOT_FOUND,
                        "Account not found"));
    }

    @Override
    public CustomerData getCustomerData(BankAccount bankAccount) {
        AccountConfig account = accounts
                .lookupAccount(bankAccount)
                .orElseThrow(() -> new BankException(
                        FAILURE_ACCOUNT_NOT_FOUND,
                        "Account not found"));

        return CustomerData.newBuilder()
                // Append to list of account holder names.
                // It's a list because there might be more than
                // one, e.g., for a joint account.
                // (Config test data doesn't have any joint accounts.)
                .addLegalNames(account.getName())
                .setAddress(account.getAddress())
                .build();
    }

    @Override
    public Optional<Transaction> getTransaction(BankAccount account, String transactionId) {
        return accounts
                .lookupTransaction(account, transactionId)
                .map(AccountTransaction::toTransaction);
    }

    @Override
    public List<Transaction> getTransactions(BankAccount account, int offset, int limit) {
        return accounts
                .lookupTransactions(account, offset, limit)
                .stream()
                .map(AccountTransaction::toTransaction)
                .collect(toList());
    }

    @Override
    public List<BankAccount> resolveTransferDestination(BankAccount account) {
        accounts.lookupAccount(account)
                .orElseThrow(() -> new BankException(
                        FAILURE_ACCOUNT_NOT_FOUND,
                        "Account not found"));
        List<BankAccount> accounts = new ArrayList<>();
        accounts.add(account);

        // For a bank that supports more than one way to transfer,
        // this list would have more than one item.
        // This simple sample only does Swift. But a bank
        // that supports other transfer-methods can return more:
        // switch (account.getAccountCase()) {
        //     case SWIFT: {
        //         BankAccount otherAccount = BankAccount.
        //                 newBuilder().
        //                 setSepa(Sepa.newBuilder(). ...).
        //                 build();
        //         accounts.add(otherAccount);
        //     }
        //     case SEPA: {
        //         BankAccount otherAccount = BankAccount.
        //                 newBuilder().
        //                 setSwift(Swift.newBuilder() ...).
        //                 build();
        //         accounts.add(otherAccount);
        //     }
        // }

        return accounts;
    }
}

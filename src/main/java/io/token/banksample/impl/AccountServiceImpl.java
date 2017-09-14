package io.token.banksample.impl;

import static io.grpc.Status.NOT_FOUND;
import static io.token.proto.common.account.AccountProtos.BankAccount.AccountCase.SWIFT;

import io.token.banksample.config.Account;
import io.token.banksample.model.Accounting;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.BankAccount.AccountCase;
import io.token.proto.common.account.AccountProtos.BankAccount.Swift;
import io.token.proto.common.money.MoneyProtos.Money;
import io.token.proto.common.transaction.TransactionProtos.Transaction;
import io.token.proto.common.transaction.TransactionProtos.TransactionStatus;
import io.token.proto.common.transaction.TransactionProtos.TransactionType;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.CustomerData;
import io.token.sdk.api.Balance;
import io.token.sdk.api.service.AccountService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample implementation of the {@link AccountService}. Returns fake data.
 */
public class AccountServiceImpl implements AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);

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
    public Optional<Transaction> getTransaction(BankAccount account, String transactionId) {
        Swift customerAccount = getSwiftAccount(account);
        logger.info("Customer Swift account: {}; BIC: {}",
                customerAccount.getAccount(), customerAccount.getBic());
        logger.info("Example transaction id: {}", transactionId);
        logger.warn("Need to verify that the transaction belongs to the passed in account");

        Transaction.newBuilder()
                .setId("transaction_id")
                .setAmount(Money.newBuilder()
                        .setValue("10.0")
                        .setCurrency("EUR"))
                .setDescription("Description")
                .setType(TransactionType.DEBIT) // For debit leg, CREDIT for credit leg.
                .setStatus(TransactionStatus.SUCCESS) // For completed transaction, TransactionStatus.PROCESSING if the transaction has not been committed yet.
                .build();
        return Optional.empty();
    }

    @Override
    public List<Transaction> getTransactions(BankAccount account, int offset, int limit) {
        Swift customerAccount = getSwiftAccount(account);
        logger.info("Customer Swift account: {}; BIC: {}",
                customerAccount.getAccount(), customerAccount.getBic());

        return Collections.emptyList();
    }

    @Override
    public CustomerData getCustomerData(BankAccount bankAccount) {
        Account account = accounts
                .lookupAccount(bankAccount)
                .orElseThrow(() -> NOT_FOUND
                        .withDescription("Account not found")
                        .asRuntimeException());

        return CustomerData.newBuilder()
                .addLegalNames(account.getName())
                .setAddress(account.getAddress())
                .build();
    }

    private Swift getSwiftAccount(BankAccount bankAccount) {
        AccountCase accountCase = bankAccount.getAccountCase();
        if (accountCase == SWIFT) {
            return bankAccount.getSwift();
        }
        throw new IllegalArgumentException("Unsupported Account Type: " + accountCase.name());
    }
}

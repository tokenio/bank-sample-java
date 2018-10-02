package io.token.banksample.model.impl;

import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toMap;

import com.google.common.base.Preconditions;
import io.token.banksample.config.AccountConfig;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Accounts;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration based account service implementation.
 */
public final class AccountingImpl implements Accounting {
    private final Accounts config;
    private final Map<AccountConfig, Account> accounts;
    private final AccountingLedger ledger;

    public AccountingImpl(Accounts config) {
        this.config = config;
        this.accounts = config.getAllAccounts()
                .stream()
                .collect(toMap(
                        a -> a,
                        a -> new Account(
                                a.getBalance().getCurrency(),
                                a.getBalance().getAvailable().doubleValue(),
                                a.getBalance().getCurrent().doubleValue())));
        this.ledger = new AccountingLedger();
    }

    @Override
    public synchronized Optional<AccountConfig> lookupAccount(BankAccount account) {
        return config.tryLookupAccount(account);
    }

    @Override
    public synchronized Optional<Balance> lookupBalance(BankAccount account) {
        return config
                .tryLookupAccount(account)
                .flatMap(a -> Optional.ofNullable(accounts.get(a)))
                .map(Account::getBalance);
    }

    @Override
    public synchronized void createDebitTransaction(AccountTransaction transaction) {
        Preconditions.checkArgument(transaction.getType() == DEBIT);
        if (!createTransaction(transaction)) {
            return;
        }

        if (transaction.getCurrency().equals(transaction.getTransferCurrency())) {
            // If FX is not needed, just move the money to the holding account.
            ledger.post(AccountTransfer.builder()
                    .from(transaction.getFrom())
                    .to(config.getHoldAccount(transaction.getCurrency()))
                    .withAmount(
                            transaction.getAmount(),
                            transaction.getCurrency())
                    .build());
        } else {
            // With FX.
            // Create two transfers to account for FX.
            // 1) DB customer, credit FX in the customer account currency.
            // 2) DB FX, credit hold account in the settlement account currency.
            // Note that we are not accounting for the spread with this
            // transaction pair, it goes 'nowhere'.
            ledger.post(
                    AccountTransfer.builder()
                            .from(transaction.getFrom())
                            .to(config.getFxAccount(transaction.getCurrency()))
                            .withAmount(
                                    transaction.getAmount(),
                                    transaction.getCurrency())
                            .build(),
                    AccountTransfer.builder()
                            .from(config.getFxAccount(transaction.getTransferCurrency()))
                            .to(config.getHoldAccount(transaction.getTransferCurrency()))
                            .withAmount(
                                    transaction.getTransferAmount(),
                                    transaction.getTransferCurrency())
                            .build());
        }
    }

    @Override
    public synchronized Optional<AccountTransaction> lookupTransaction(
            BankAccount account,
            String transactionId) {
        return config
                .tryLookupAccount(account)
                .flatMap(a -> Optional.ofNullable(accounts.get(a)))
                .flatMap(a -> a.lookupTransaction(transactionId));
    }

    @Override
    public synchronized List<AccountTransaction> lookupTransactions(
            BankAccount account,
            int offset,
            int limit) {
        return Optional
                .ofNullable(config.lookupAccount(account))
                .flatMap(a -> Optional.ofNullable(accounts.get(a)))
                .map(a -> a.lookupTransactions(offset, limit))
                .orElse(emptyList());
    }

    private boolean createTransaction(AccountTransaction transaction) {
        return accounts
                .get(config.lookupAccount(transaction.getFrom()))
                .createTransaction(transaction);
    }
}

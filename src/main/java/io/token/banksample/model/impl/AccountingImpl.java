package io.token.banksample.model.impl;

import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.FAILURE_CANCELED;
import static io.token.proto.common.transaction.TransactionProtos.TransactionStatus.SUCCESS;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.CREDIT;
import static io.token.proto.common.transaction.TransactionProtos.TransactionType.DEBIT;
import static java.util.stream.Collectors.toMap;

import com.google.common.base.Preconditions;
import io.grpc.Status;
import io.token.banksample.config.AccountConfig;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Accounts;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.TransferException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration based account service implementation.
 *
 * TODO: Account limits are not enforced at this point
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
                        a -> new Account()));
        this.ledger = new AccountingLedger();
    }

    @Override
    public synchronized Optional<AccountConfig> lookupAccount(BankAccount account) {
        return config.tryLookupAccount(account);
    }

    @Override
    public synchronized void createDebitTransaction(AccountTransaction transaction) {
        Preconditions.checkArgument(transaction.getType() == DEBIT);
        createTransaction(transaction);

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
            // Note that we are not accounting for  the spread with this
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
    public synchronized void createCreditTransaction(AccountTransaction transaction) {
        Preconditions.checkArgument(transaction.getType() == CREDIT);
        createTransaction(transaction);
    }

    @Override
    public synchronized void commitDebitTransaction(
            BankAccount account,
            String transferId,
            String transactionId) {
        AccountTransaction transaction = lookupTransactionOrThrow(account, transactionId);
        transaction.setStatus(SUCCESS);
        ledger.post(AccountTransfer.builder()
                .transferId(transferId)
                .from(transaction.getTo())
                .to(config.getSettlementAccount(transaction.getCurrency()))
                .withAmount(transaction.getAmount(), transaction.getCurrency())
                .build());
    }

    @Override
    public synchronized void commitCreditTransaction(
            BankAccount account,
            String transferId,
            String transactionId) {
        AccountTransaction transaction = lookupTransactionOrThrow(account, transactionId);
        transaction.setStatus(SUCCESS);
        ledger.post(AccountTransfer.builder()
                .transferId(transferId)
                .from(config.getSettlementAccount(transaction.getCurrency()))
                .to(account)
                .withAmount(transaction.getAmount(), transaction.getCurrency())
                .build());
    }

    @Override
    public synchronized void rollbackDebitTransaction(
            BankAccount account,
            String transferId,
            String transactionId) {
        lookupTransaction(account, transactionId)
                .ifPresent(transaction -> {
                    transaction.setStatus(FAILURE_CANCELED);
                    ledger.post(AccountTransfer.builder()
                            .transferId(transferId)
                            .from(transaction.getTo())
                            .to(transaction.getFrom())
                            .withAmount(transaction.getAmount(), transaction.getCurrency())
                            .build());
                });
    }

    @Override
    public synchronized void rollbackCreditTransaction(
            BankAccount account,
            String transferId,
            String transactionId) {
        lookupTransaction(account, transactionId)
                .ifPresent(transaction -> transaction.setStatus(FAILURE_CANCELED));
    }

    @Override
    public synchronized Optional<AccountTransaction> lookupTransaction(
            BankAccount account,
            String transactionId) {
        return config
                .tryLookupAccount(account)
                .flatMap(a -> accounts
                        .getOrDefault(a, new Account())
                        .lookupTransaction(transactionId));
    }

    @Override
    public synchronized List<AccountTransaction> lookupTransactions(
            BankAccount account,
            int offset,
            int limit) {
        return accounts
                .getOrDefault(config.lookupAccount(account), new Account())
                .lookupTransactions(offset, limit);
    }

    private void createTransaction(AccountTransaction transaction) {
        AccountConfig account = config.lookupAccount(transaction.getFrom());
        if (account.matches(config.getRejectAccount(transaction.getCurrency()))) {
            throw new TransferException(FAILURE_CANCELED, "Reject account - cancelled");
        }
        accounts
                .getOrDefault(account, new Account())
                .createTransaction(transaction);
    }

    /**
     * Looks up transaction given the account and transaction ID.
     *
     * @param account account to lookup the transaction for
     * @param transactionId transaction id
     * @return looked up transaction
     */
    private AccountTransaction lookupTransactionOrThrow(BankAccount account, String transactionId) {
        return this
                .lookupTransaction(account, transactionId)
                .orElseThrow(() -> Status
                        .NOT_FOUND
                        .withDescription("AccountTransaction not found: " + transactionId)
                        .asRuntimeException());
    }
}

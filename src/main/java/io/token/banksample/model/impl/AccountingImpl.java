package io.token.banksample.model.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.token.banksample.config.Account;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.AccountTransactionPair;
import io.token.banksample.model.AccountTransfer;
import io.token.banksample.model.Accounting;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.PrepareTransferException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration based account service implementation.
 *
 * TODO: Account limits are not enforced at this point
 */
public final class AccountingImpl implements Accounting {
    private final Map<String, Account> holdAccounts;
    private final Map<String, Account> settlementAccounts;
    private final Map<String, Account> fxAccounts;
    private final Collection<Account> accounts;
    private final Map<String, AccountTransfer> transfers;
    private final Map<Account, AccountLedger> transactions;

    public AccountingImpl(
            Collection<Account> holdAccounts,
            Collection<Account> settlementAccounts,
            Collection<Account> fxAccounts,
            Collection<Account> customerAccounts) {
        this.holdAccounts = indexAccounts(holdAccounts);
        this.settlementAccounts = indexAccounts(settlementAccounts);
        this.fxAccounts = indexAccounts(fxAccounts);

        this.accounts = new ArrayList<>();
        this.accounts.addAll(holdAccounts);
        this.accounts.addAll(settlementAccounts);
        this.accounts.addAll(fxAccounts);
        this.accounts.addAll(customerAccounts);

        this.transfers = new HashMap<>();
        this.transactions = new HashMap<>();
    }

    @Override
    public synchronized BankAccount getHoldAccount(String currency) {
        return Optional
                .ofNullable(holdAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Hold account is not found for: " + currency));
    }

    @Override
    public synchronized BankAccount getSettlementAccount(String currency) {
        return Optional
                .ofNullable(settlementAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Settlement account is not found for: " + currency));
    }

    @Override
    public synchronized BankAccount getFxAccount(String currency) {
        return Optional
                .ofNullable(fxAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "FX account is not found for: " + currency));
    }

    @Override
    public synchronized Optional<Account> lookupAccount(BankAccount account) {
        return toSwiftAccount(account)
                .flatMap(swift -> accounts.stream()
                        .filter(a -> a.getBic().equals(swift.getBic()))
                        .filter(a -> a.getNumber().equals(swift.getAccount()))
                        .findFirst());
    }

    @Override
    public synchronized List<AccountTransactionPair> transfer(List<AccountTransfer> newTransfers) {
        return newTransfers.stream()
                .map(t -> {
                    transfers.put(t.getTransferId(), t);
                    Account from = lookupAccountOrThrow(t.getFrom());
                    Account to = lookupAccountOrThrow(t.getTo());
                    transactions.putIfAbsent(from, new AccountLedger());
                    transactions.putIfAbsent(to, new AccountLedger());
                    transactions.get(from).addTransaction(t.toTransactionPair().getDebit());
                    transactions.get(to).addTransaction(t.toTransactionPair().getCredit());
                    return t.toTransactionPair();
                })
                .collect(toList());
    }

    @Override
    public synchronized Optional<AccountTransfer> lookupTransfer(String transferId) {
        return Optional.ofNullable(transfers.get(transferId));
    }

    @Override
    public synchronized Optional<AccountTransaction> lookupTransaction(
            BankAccount account,
            String transactionId) {
        Account lookedUpAccount = lookupAccountOrThrow(account);
        return transactions
                .getOrDefault(lookedUpAccount, new AccountLedger())
                .lookupTransaction(transactionId);
    }

    @Override
    public synchronized List<AccountTransaction> lookupTransactions(
            BankAccount account,
            int offset,
            int limit) {
        Account lookedUpAccount = lookupAccountOrThrow(account);
        return transactions
                .getOrDefault(lookedUpAccount, new AccountLedger())
                .lookupTransactions(offset, limit);
    }

    private Account lookupAccountOrThrow(BankAccount account) {
        return lookupAccount(account)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Account not found"));
    }

    private static Map<String, Account> indexAccounts(Collection<Account> accounts) {
        return accounts
                .stream()
                .collect(toMap(
                        a -> a.getBalance().getCurrency(),
                        a -> a));
    }

    private static Optional<BankAccount.Swift> toSwiftAccount(BankAccount account) {
        if (account.getAccountCase() != BankAccount.AccountCase.SWIFT) {
            return Optional.empty();
        } else {
            return Optional.of(account.getSwift());
        }
    }
}

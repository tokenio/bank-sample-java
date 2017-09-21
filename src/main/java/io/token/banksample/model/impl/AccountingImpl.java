package io.token.banksample.model.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND;
import static java.util.stream.Collectors.toMap;

import io.token.banksample.config.Account;
import io.token.banksample.model.AccountTransaction;
import io.token.banksample.model.AccountTransfer;
import io.token.banksample.model.Accounting;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.PrepareTransferException;

import java.util.ArrayList;
import java.util.Collection;
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
    private final Map<String, Account> rejectAccounts;
    private final Map<Account, AccountLedger> accounts;

    public AccountingImpl(
            Collection<Account> holdAccounts,
            Collection<Account> settlementAccounts,
            Collection<Account> fxAccounts,
            Collection<Account> rejectAccounts,
            Collection<Account> customerAccounts) {
        this.holdAccounts = indexAccounts(holdAccounts);
        this.settlementAccounts = indexAccounts(settlementAccounts);
        this.fxAccounts = indexAccounts(fxAccounts);
        this.rejectAccounts = indexAccounts(rejectAccounts);
        this.accounts =
                new ArrayList<Account>() {{
                    addAll(holdAccounts);
                    addAll(settlementAccounts);
                    addAll(fxAccounts);
                    addAll(customerAccounts);
                    addAll(rejectAccounts);
                }}
                        .stream()
                        .collect(toMap(
                                a -> a,
                                a -> new AccountLedger()));
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
    public synchronized BankAccount getRejectAccount(String currency) {
        return Optional
                .ofNullable(rejectAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Reject account is not found for: " + currency));
    }

    @Override
    public synchronized Optional<Account> lookupAccount(BankAccount account) {
        return toSwiftAccount(account)
                .flatMap(swift -> accounts.keySet().stream()
                        .filter(a -> a.getBic().equals(swift.getBic()))
                        .filter(a -> a.getNumber().equals(swift.getAccount()))
                        .findFirst());
    }

    @Override
    public synchronized void createPayment(AccountTransaction transaction) {
        Account account = lookupAccountOrThrow(transaction.getFrom());
        AccountLedger ledger = accounts.get(account);
        ledger.createPayment(transaction);
    }

    @Override
    public synchronized Optional<AccountTransaction> tryLookupPayment(
            BankAccount account,
            String paymentId) {
        return accounts
                .get(lookupAccountOrThrow(account))
                .lookupPayment(paymentId);
    }

    @Override
    public synchronized List<AccountTransaction> lookupPayments(
            BankAccount account,
            int offset,
            int limit) {
        return accounts
                .get(lookupAccountOrThrow(account))
                .lookupPayments(offset, limit);
    }

    @Override
    public synchronized void deletePayment(BankAccount account, String paymentId) {
        accounts
                .get(lookupAccountOrThrow(account))
                .deletePayment(paymentId);
    }

    @Override
    public void post(AccountTransfer... transfers) {
        for (AccountTransfer transfer : transfers) {
            AccountLedgerEntry debit = AccountLedgerEntry.debit(transfer);
            AccountLedgerEntry credit = AccountLedgerEntry.credit(transfer);
            accounts.get(lookupAccountOrThrow(debit.getAccount())).post(debit);
            accounts.get(lookupAccountOrThrow(credit.getAccount())).post(credit);
        }
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

package io.token.banksample.model.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND;
import static java.util.stream.Collectors.toMap;

import io.token.banksample.config.Account;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Payment;
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
    private final Map<String, Account> rejectAccounts;
    private final Collection<Account> accounts;
    private final Map<Account, AccountLedger> payments;

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

        this.accounts = new ArrayList<>();
        this.accounts.addAll(holdAccounts);
        this.accounts.addAll(settlementAccounts);
        this.accounts.addAll(fxAccounts);
        this.accounts.addAll(customerAccounts);
        this.accounts.addAll(rejectAccounts);

        this.payments = new HashMap<>();
        this.accounts.forEach(a -> this.payments.put(a, new AccountLedger()));
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
                .flatMap(swift -> accounts.stream()
                        .filter(a -> a.getBic().equals(swift.getBic()))
                        .filter(a -> a.getNumber().equals(swift.getAccount()))
                        .findFirst());
    }

    @Override
    public synchronized void createPayment(Payment payment) {
        Account account = lookupAccountOrThrow(payment.getFrom());
        AccountLedger ledger = payments.get(account);
        ledger.createPayment(payment);
    }

    @Override
    public synchronized Optional<Payment> lookupPayment(
            BankAccount account,
            String paymentId) {
        Account lookedUpAccount = lookupAccountOrThrow(account);
        return payments
                .getOrDefault(lookedUpAccount, new AccountLedger())
                .lookupPayment(paymentId);
    }

    @Override
    public synchronized List<Payment> lookupPayments(
            BankAccount account,
            int offset,
            int limit) {
        Account lookedUpAccount = lookupAccountOrThrow(account);
        return payments
                .getOrDefault(lookedUpAccount, new AccountLedger())
                .lookupPayments(offset, limit);
    }

    @Override
    public synchronized void deletePayment(BankAccount account, String paymentId) {
        payments
                .get(lookupAccountOrThrow(account))
                .deletePayment(paymentId);
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

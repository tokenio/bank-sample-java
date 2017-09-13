package io.token.banksample.model.impl;

import static io.token.proto.common.token.TokenProtos.TransferTokenStatus.FAILURE_SOURCE_ACCOUNT_NOT_FOUND;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.token.banksample.config.Account;
import io.token.banksample.model.AccountTransactionPair;
import io.token.banksample.model.AccountTransfer;
import io.token.banksample.model.Accounting;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;
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
public class AccountingImpl implements Accounting {
    private final Map<String, Account> holdAccounts;
    private final Map<String, Account> settlementAccounts;
    private final Map<String, Account> fxAccounts;
    private final Collection<Account> accounts;
    private final Map<String, AccountTransfer> transfers;

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
    }

    @Override
    public BankAccount getHoldAccount(String currency) {
        return Optional
                .ofNullable(holdAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Hold account is not found for: " + currency));
    }

    @Override
    public BankAccount getSettlementAccount(String currency) {
        return Optional
                .ofNullable(settlementAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "Settlement account is not found for: " + currency));
    }

    @Override
    public BankAccount getFxAccount(String currency) {
        return Optional
                .ofNullable(fxAccounts.get(currency))
                .map(Account::toBankAccount)
                .orElseThrow(() -> new PrepareTransferException(
                        FAILURE_SOURCE_ACCOUNT_NOT_FOUND,
                        "FX account is not found for: " + currency));
    }

    @Override
    public Optional<Balance> lookupBalance(BankAccount account) {
        return toSwiftAccount(account)
                .flatMap(swift -> accounts.stream()
                        .filter(a -> a.getBic().equals(swift.getBic()))
                        .filter(a -> a.getNumber().equals(swift.getAccount()))
                        .map(Account::getBalance)
                        .findFirst());
    }

    @Override
    public List<AccountTransactionPair> transfer(List<AccountTransfer> newTransfers) {
        return newTransfers.stream()
                .map(t -> {
                    transfers.put(t.getTransferId(), t);
                    return t.toTransactionPair();
                })
                .collect(toList());
    }

    @Override
    public Optional<AccountTransfer> lookupTransfer(String transferId) {
        return Optional.ofNullable(transfers.get(transferId));
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

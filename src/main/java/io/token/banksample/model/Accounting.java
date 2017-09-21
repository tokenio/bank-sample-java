package io.token.banksample.model;

import io.grpc.Status;
import io.token.banksample.config.Account;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.List;
import java.util.Optional;

/**
 * AccountTransaction accounting service. Abstracts away bank account data store.
 */
public interface Accounting {
    /**
     * Returns hold account for a given currency.
     *
     * @param currency currency
     * @return looked up hold account
     */
    BankAccount getHoldAccount(String currency);

    /**
     * Returns settlement account for a given currency.
     *
     * @param currency currency
     * @return looked up settlement account
     */
    BankAccount getSettlementAccount(String currency);

    /**
     * Returns FX account for a given currency.
     *
     * @param currency currency
     * @return looked up FX account
     */
    BankAccount getFxAccount(String currency);

    /**
     * Returns FX account for a given currency.
     *
     * @param currency currency
     * @return looked up FX account
     */
    BankAccount getRejectAccount(String currency);

    /**
     * Looks up account information.
     *
     * @param bankAccount account to lookup the info for
     * @return account info
     */
    Optional<Account> lookupAccount(BankAccount bankAccount);

    /**
     * Looks up account balance.
     *
     * @param account account to lookup the balance for
     * @return account balance if found
     */
    default Optional<Balance> lookupBalance(BankAccount account) {
        return lookupAccount(account).map(Account::getBalance);
    }

    /**
     * Creates a new transaction.
     *
     * @param transaction new transaction
     */
    void createPayment(AccountTransaction transaction);

    /**
     * Looks up payment given the account and payment ID.
     *
     * @param account account to lookup the payment for
     * @param paymentId payment id
     * @return looked up payment
     */
    default AccountTransaction lookupPayment(BankAccount account, String paymentId) {
        return tryLookupPayment(account, paymentId).orElseThrow(() ->
                Status
                        .NOT_FOUND
                        .withDescription("AccountTransaction not found: " + paymentId)
                        .asRuntimeException());
    }

    /**
     * Looks up payment given the account and payment ID.
     *
     * @param account account to lookup the payment for
     * @param paymentId payment id
     * @return looked up payment if found
     */
    Optional<AccountTransaction> tryLookupPayment(BankAccount account, String paymentId);

    /**
     * Looks up payments for the given account.
     *
     * @param account account to lookup the payments for
     * @param offset the result offset
     * @param limit the limit on the number of results returned
     * @return list of looked up payments
     */
    List<AccountTransaction> lookupPayments(BankAccount account, int offset, int limit);

    /**
     * Deletes an existing payment.
     *
     * @param account account that the payment belongs to
     * @param paymentId payment id
     */
    void deletePayment(BankAccount account, String paymentId);

    /**
     * Posts account transactions for a given post.
     *
     * @param transfers list of transfers to post
     */
    void post(AccountTransfer ... transfers);
}

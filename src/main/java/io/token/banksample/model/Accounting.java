package io.token.banksample.model;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.Optional;

/**
 * Transaction accounting service. Abstracts away bank account data store.
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
     * Looks up account balance.
     *
     * @param account account to lookup the balance for
     * @return account balance if found
     */
    Optional<Balance> lookupBalance(BankAccount account);

    /**
     * Posts the transfer to the specified accounts. The transfer results
     * in 2 {@link AccountTransaction}s, one for debit and one for credit.
     *
     * @param transfer transfer instructions
     * @return debit and credit transactions
     */
    AccountTransactionPair transfer(AccountTransfer transfer);

    /**
     * Looks up an existing transfer.
     *
     * @param transferId transfer id
     * @return transfer object
     */
    Optional<AccountTransfer> lookupTransfer(String transferId);
}

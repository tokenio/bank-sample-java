package io.token.banksample.model;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.List;
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
     * Returns FX account for a given currency.
     *
     * @param currency currency
     * @return looked up FX account
     */
    BankAccount getFxAccount(String currency);

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
    default AccountTransactionPair transfer(AccountTransfer transfer) {
        return transfer(singletonList(transfer)).get(0);
    }

    /**
     * Posts the transfer to the specified accounts. Each transfer results
     * in 2 {@link AccountTransaction}s, one for debit and one for credit.
     *
     * @param transfers transfer instructions
     * @return debit and credit transactions
     */
    default List<AccountTransactionPair> transfer(AccountTransfer ... transfers) {
       return transfer(asList(transfers));
    }

    /**
     * Posts the transfer to the specified accounts. Each transfer results
     * in 2 {@link AccountTransaction}s, one for debit and one for credit.
     *
     * @param transfers transfer instructions
     * @return debit and credit transactions
     */
    List<AccountTransactionPair> transfer(List<AccountTransfer> transfers);

    /**
     * Looks up an existing transfer.
     *
     * @param transferId transfer id
     * @return transfer object
     */
    Optional<AccountTransfer> lookupTransfer(String transferId);
}

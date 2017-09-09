package io.token.banksample.model;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.sdk.api.Balance;

import java.util.Optional;

/**
 * Account lookup service. Abstracts away bank account data store.
 */
public interface Accounts {
    /**
     * Looks up account balance.
     *
     * @param account account to lookup the balance for
     * @return account balance if found
     */
    Optional<Balance> lookupBalance(BankAccount account);
}

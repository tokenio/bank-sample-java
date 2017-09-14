package io.token.banksample.config;

import com.google.auto.value.AutoValue;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.sdk.api.Balance;

import java.math.BigDecimal;

/**
 * A bank account.
 */
@AutoValue
public abstract class Account {
    /**
     * Creates new bank account data structure.
     *
     * @param name account legal name
     * @param address account physical address
     * @param bic account BIC
     * @param number account number
     * @param currency account currency
     * @param balance account balance
     * @return newly created account
     */
    static Account create(
            String name,
            Address address,
            String bic,
            String number,
            String currency,
            double balance) {
        return create(
                name,
                address,
                bic,
                number,
                Balance.create(
                        currency,
                        BigDecimal.valueOf(balance),
                        BigDecimal.valueOf(balance)));
    }

    /**
     * Creates new bank account data structure.
     *
     * @param name account legal name
     * @param address account physical address
     * @param bic account BIC
     * @param number account number
     * @param balance account balance
     * @return newly created account
     */
    static Account create(
            String name,
            Address address,
            String bic,
            String number,
            Balance balance) {
        return new AutoValue_Account(name, address, bic, number, balance);
    }

    /**
     * Returns account legal name.
     *
     * @return account name
     */
    public abstract String getName();

    /**
     * Returns account physical address
     *
     * @return account address
     */
    public abstract Address getAddress();

    /**
     * Returns account BIC.
     *
     * @return account BIC
     */
    public abstract String getBic();

    /**
     * Returns account number.
     *
     * @return account number
     */
    public abstract String getNumber();

    /**
     * Returns account balance.
     *
     * @return account balance
     */
    public abstract Balance getBalance();

    /**
     * Helper method to convert this object to the proto account definition.
     *
     * @return proto account definition
     */
    public BankAccount toBankAccount() {
        return BankAccount.newBuilder()
                .setSwift(BankAccount.Swift.newBuilder()
                        .setBic(getBic())
                        .setAccount(getNumber())
                        .build())
                .build();
    }
}

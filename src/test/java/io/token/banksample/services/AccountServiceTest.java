package io.token.banksample.services;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.banksample.Factory;
import io.token.proto.common.account.AccountProtos;
import io.token.sdk.api.Balance;
import io.token.sdk.api.service.AccountService;

import org.junit.Before;
import org.junit.Test;

public class AccountServiceTest {
    private AccountService accountService;

    @Before
    public void before() {
        Factory factory = new Factory("config/application.conf");
        accountService = factory.accountService();
    }

    @Test
    public void getBalance() {
        Balance balance = accountService.getBalance(AccountProtos.BankAccount.newBuilder()
                .setSwift(AccountProtos.BankAccount.Swift.newBuilder()
                        .setBic("RUBYUSCA000")
                        .setAccount("0000001"))
                .build());

        assertThat(balance.getAvailable().doubleValue())
                .isEqualTo(1000000.);
        assertThat(balance.getCurrency()).isEqualTo("USD");
    }
}

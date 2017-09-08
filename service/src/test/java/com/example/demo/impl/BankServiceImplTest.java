package com.example.demo.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.pricing.PricingProtos.TransferQuote;
import io.token.proto.common.token.TokenProtos.TransferTokenStatus;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.CustomerData;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.PurposeOfPayment;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos.TransferEndpoint;
import io.token.sdk.api.PrepareTransferException;

import java.math.BigDecimal;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

public class BankServiceImplTest {
    private BankServiceImpl bankService;

    @Before
    public void before() {
        bankService = new BankServiceImpl();
    }

    private BankAccount getTestAccount1AsBankAccount() {
        return BankAccount.newBuilder()
                .setSwift(BankAccount.Swift.newBuilder()
                        .setBic("bic")
                        .setAccount("TokenTest0001"))
                .build();
    }

    private TransferEndpoint getTestAccount1AsTransferEndpoint() {
        BankAccount account = getTestAccount1AsBankAccount();

        return TransferEndpoint.newBuilder()
                .setAccount(account)
                .setCustomerData(CustomerData.newBuilder()
                        .addLegalNames("Token Test Sr.")
                        .setAddress(Address.newBuilder()
                                .setHouseNumber("1")
                                .setStreet("Platz der Republik")
                                .setPostCode("11011")
                                .setCity("Berlin")))
                .build();
    }

    private TransferEndpoint getTestAccount2AsTransferEndpoint() {
        return TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setSwift(BankAccount.Swift.newBuilder()
                                .setBic("bic")
                                .setAccount("TokenTest0002")))
                .setCustomerData(CustomerData.newBuilder()
                        .addLegalNames("Token Test Jr.")
                        .setAddress(Address.newBuilder()
                                .setHouseNumber("2")
                                .setStreet("Platz der Republik")
                                .setPostCode("11011")
                                .setCity("Berlin")))
                .build();
    }

    @Test
    public void prepareCreditDebitSameBank() {
        BankAccount payerAccount = getTestAccount1AsBankAccount();
        TransferEndpoint payerEndpoint = getTestAccount1AsTransferEndpoint();
        TransferEndpoint payeeEndpoint = getTestAccount2AsTransferEndpoint();

        String tokenRefId = RandomStringUtils.random(64);
        BigDecimal amount = BigDecimal.valueOf(10l);
        String currency = "EUR";
        PurposeOfPayment purposeOfPayment = PurposeOfPayment.FAMILY_MAINTENANCE;

        TransferQuote creditQuote = bankService.prepareCredit(
                tokenRefId,
                amount,
                currency,
                payerEndpoint,
                payeeEndpoint,
                purposeOfPayment,
                Optional.empty());

        assertThat(creditQuote.getAccountCurrency()).isEqualTo("EUR");
        assertThat(creditQuote.getRatesCount()).isZero();

        TransferQuote debitQuote = bankService.prepareDebit(
                tokenRefId,
                amount,
                currency,
                payerAccount,
                payeeEndpoint,
                creditQuote,
                purposeOfPayment,
                Optional.empty());

        assertThat(debitQuote.getAccountCurrency()).isEqualTo("EUR");
        assertThat(debitQuote.getRatesCount()).isZero();

        assertThat(creditQuote.getId()).isNotEqualTo(debitQuote.getId());
    }

    @Test
    public void prepareDebitTooMuchShouldThrow() {
        BankAccount payerAccount = getTestAccount1AsBankAccount();
        TransferEndpoint payerEndpoint = getTestAccount1AsTransferEndpoint();
        TransferEndpoint payeeEndpoint = getTestAccount2AsTransferEndpoint();

        String tokenRefId = RandomStringUtils.random(64);
        BigDecimal amount = BigDecimal.valueOf(1000l); // too much; TokenTest0001 only has 100 EUR
        String currency = "EUR";
        PurposeOfPayment purposeOfPayment = PurposeOfPayment.FAMILY_MAINTENANCE;

        TransferQuote creditQuote = bankService.prepareCredit(
                tokenRefId,
                amount,
                currency,
                payerEndpoint,
                payeeEndpoint,
                purposeOfPayment,
                Optional.empty());

        assertThatExceptionOfType(PrepareTransferException.class)
                .isThrownBy(() -> bankService.prepareDebit(
                        tokenRefId,
                        amount,
                        currency,
                        payerAccount,
                        payeeEndpoint,
                        creditQuote,
                        purposeOfPayment,
                        Optional.empty()))
                .matches(e -> e.getStatus() == TransferTokenStatus.FAILURE_INSUFFICIENT_FUNDS);
    }

    @Test
    public void prepareCreditWithFx() {
        TransferEndpoint payerEndpoint = TransferEndpoint.newBuilder()
                .setAccount(BankAccount.newBuilder()
                        .setSwift(BankAccount.Swift.newBuilder()
                                .setBic("iron")
                                .setAccount("CHK FE 1781 5233 8610")))
                .setCustomerData(CustomerData.newBuilder()
                        .addLegalNames("Jon Snow")
                        .setAddress(Address.newBuilder()
                                .setStreet("East Capital St NE")
                                .setPostCode("20004")
                                .setCity("Washington DC")
                                .setCountry("USA")))
                .build();

        String tokenRefId = RandomStringUtils.random(64);
        BigDecimal amount = BigDecimal.valueOf(100l);
        String currency = "USD";
        PurposeOfPayment purposeOfPayment = PurposeOfPayment.PERSONAL_EXPENSES;

        TransferEndpoint payeeEndpoint = getTestAccount2AsTransferEndpoint();

        TransferQuote creditQuote = bankService.prepareCredit(
                tokenRefId,
                amount,
                currency,
                payerEndpoint,
                payeeEndpoint,
                purposeOfPayment,
                Optional.empty());

        assertThat(creditQuote.getRatesCount()).isOne();
        TransferQuote.FxRate fxRate = creditQuote.getRates(0);
        assertThat(fxRate.getBaseCurrency()).isEqualTo("USD");
        assertThat(fxRate.getQuoteCurrency()).isEqualTo("EUR");
        Float ratio = Float.parseFloat(fxRate.getRate());
        // if USD goes > 1.1 EUR, we need to change our test, oh well
        assertThat(ratio).isLessThan(Float.parseFloat("1.1"));
    }

    @Test
    public void prepareCreditRepeated() {
        // TokenOS might call prepareCredit() more than once about the same thing.
        // This might happen, for example, if network connectivity doesn't let our
        // response reach the Token cloud.

        // We should return the same value, not generate a new quote.

        TransferEndpoint payerEndpoint = getTestAccount1AsTransferEndpoint();
        TransferEndpoint payeeEndpoint = getTestAccount2AsTransferEndpoint();

        String tokenRefId = RandomStringUtils.random(64);
        BigDecimal amount = BigDecimal.valueOf(10l);
        String currency = "EUR";
        PurposeOfPayment purposeOfPayment = PurposeOfPayment.FAMILY_MAINTENANCE;

        TransferQuote firstCreditQuote = bankService.prepareCredit(
                tokenRefId,
                amount,
                currency,
                payerEndpoint,
                payeeEndpoint,
                purposeOfPayment,
                Optional.empty());

        TransferQuote secondCreditQuote = bankService.prepareCredit(
                tokenRefId,
                amount,
                currency,
                payerEndpoint,
                payeeEndpoint,
                purposeOfPayment,
                Optional.empty());

        assertThat(secondCreditQuote).isEqualTo(firstCreditQuote);

        TransferQuote subtlyDifferentQuote = bankService.prepareCredit(
                RandomStringUtils.random(64),
                amount,
                currency,
                payerEndpoint,
                payeeEndpoint,
                purposeOfPayment,
                Optional.empty());

        assertThat(subtlyDifferentQuote).isNotEqualTo(firstCreditQuote);
    }
}

package io.token.banksample;

import static org.junit.Assert.*;

import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.pricing.PricingProtos;
import io.token.proto.common.transferinstructions.TransferInstructionsProtos;
import io.token.sdk.api.service.PricingService;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;


import org.junit.Test;

public class FactoryTest {
    @Test
    public void something() {
        Path configPath = new File("config").toPath().resolve("application.conf");
        Factory f = new Factory(configPath.toString());
        PricingService pricing = f.pricingService();

        pricing.prepareDebit(
                "fakeRefId",
                new BigDecimal(10000001),
                "JPY",
                AccountProtos.BankAccount.newBuilder()
                        .setSwift(AccountProtos.BankAccount.Swift.newBuilder()
                                .setBic("RUBYUSCA000")
                                .setAccount("0000002"))
                        .build(),
                TransferInstructionsProtos.TransferEndpoint.getDefaultInstance(),
                PricingProtos.TransferQuote.getDefaultInstance(),
                TransferInstructionsProtos.PurposeOfPayment.PERSONAL_EXPENSES
        );
    }
}

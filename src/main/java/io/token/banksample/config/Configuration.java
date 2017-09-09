package io.token.banksample.config;

import static java.util.stream.Collectors.toList;

import com.typesafe.config.Config;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.FxRate;

import java.math.BigDecimal;
import java.util.List;

public final class Configuration {
    private final Config config;

    public Configuration(Config config) {
        this.config = config;
    }

    public List<Account> accountList() {
        return config.getConfigList("accounts")
                .stream()
                .map(c -> Account.create(
                        c.getString("name"),
                        c.getString("bic"),
                        c.getString("number"),
                        c.getString("currency"),
                        c.getDouble("balance")))
                .collect(toList());
    }

    public List<FxRate> fxRateList() {
        Config fx = config.getConfig("pricing.FX");
        return fx.root().unwrapped()
                .keySet()
                .stream()
                .flatMap(from -> {
                    Config toConfig = fx.getConfig(from);
                    return toConfig.root().unwrapped()
                            .keySet()
                            .stream()
                            .map(to -> FxRate.newBuilder()
                                    .setBaseCurrency(from)
                                    .setQuoteCurrency(to)
                                    .setRate(toConfig.getString(to))
                                    .build());
                })
                .collect(toList());
    }

    public BigDecimal transactionFee() {
        return BigDecimal.valueOf(config.getDouble("pricing.transaction_fee"));
    }
}

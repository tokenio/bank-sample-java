package io.token.banksample.config;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.typesafe.config.Config;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.FxRate;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parses configuration file and extracts different pieces of configuration.
 */
public final class Configuration {
    private final Config config;

    /**
     * Creates new configuration object.
     *
     * @param config config to parse
     */
    public Configuration(Config config) {
        this.config = config;
    }

    /**
     * Extracts list of accounts from the config.
     *
     * @return list of configured accounts
     */
    public List<Account> accounts() {
        return config.getConfigList("accounts.customers")
                .stream()
                .map(c -> Account.create(
                        c.getString("name"),
                        c.getString("bic"),
                        c.getString("number"),
                        c.getString("currency"),
                        c.getDouble("balance")))
                .collect(toList());
    }

    /**
     * Extracts hold account info for the given currency.
     *
     * @param currency currency to extract the hold account for
     * @return hold account for the given currency
     */
    public Account holdAccountFor(String currency) {
        String bic = config.getString("accounts.hold.bic");
        String numberFormat = config.getString("accounts.hold.number_format");
        return Account.create(
                "Holding account - " + currency,
                bic,
                format(numberFormat, currency),
                currency,
                0);
    }

    /**
     * Extracts settlement account info for the given currency.
     *
     * @param currency currency to extract the settlement account for
     * @return settlement account for the given currency
     */
    public Account settlementAccountFor(String currency) {
        String bic = config.getString("accounts.settlement.bic");
        String numberFormat = config.getString("accounts.settlement.number_format");
        return Account.create(
                "Settlement account - " + currency,
                bic,
                format(numberFormat, currency),
                currency,
                0);
    }

    /**
     * FX account info for the given currency.
     *
     * @param currency currency to extract the FX account for
     * @return FX account for the given currency
     */
    public Account fxAccountFor(String currency) {
        String bic = config.getString("accounts.fx.bic");
        String numberFormat = config.getString("accounts.fx.number_format");
        return Account.create(
                "FX account - " + currency,
                bic,
                format(numberFormat, currency),
                currency,
                0);
    }

    /**
     * Extracts FX rate list.
     *
     * @return FX rates
     */
    public List<FxRate> fxRates() {
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

    /**
     * Extracts transaction fee.
     *
     * @return transaction fee, in the account currency
     */
    public BigDecimal transactionFee() {
        return BigDecimal.valueOf(config.getDouble("pricing.transaction_fee"));
    }
}

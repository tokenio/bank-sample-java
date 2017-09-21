package io.token.banksample.config;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.typesafe.config.Config;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.FxRate;

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
                .map(c -> {
                    Config address = c.getConfig("address");
                    return Account.create(
                            c.getString("name"),
                            Address.newBuilder()
                                    .setHouseNumber(address.getString("house"))
                                    .setStreet(address.getString("street"))
                                    .setCity(address.getString("city"))
                                    .setPostCode(address.getString("post_code"))
                                    .setCountry(address.getString("country"))
                                    .build(),
                            c.getString("bic"),
                            c.getString("number"),
                            c.getString("currency"),
                            c.getDouble("balance"));
                })
                .collect(toList());
    }

    /**
     * Extracts hold account info for the given currency.
     *
     * @param currency currency to extract the hold account for
     * @return hold account for the given currency
     */
    public Account holdAccountFor(String currency) {
        return accountForTemplate("hold", currency);
    }

    /**
     * Extracts settlement account info for the given currency.
     *
     * @param currency currency to extract the settlement account for
     * @return settlement account for the given currency
     */
    public Account settlementAccountFor(String currency) {
        return accountForTemplate("settlement", currency);
    }

    /**
     * FX account info for the given currency.
     *
     * @param currency currency to extract the FX account for
     * @return FX account for the given currency
     */
    public Account fxAccountFor(String currency) {
        return accountForTemplate("fx", currency);
    }

    /**
     * Reject account info for the given currency.
     *
     * @param currency currency to extract the reject account for
     * @return FX account for the given currency
     */
    public Account rejectAccountFor(String currency) {
        return accountForTemplate("reject", currency);
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

    private Account accountForTemplate(String pattern, String currency) {
        Config accountConfig = config.getConfig("accounts." + pattern);

        String bic = accountConfig.getString("bic");
        String numberFormat = accountConfig.getString("number_format");
        double balance = accountConfig.hasPath("balance")
                ? accountConfig.getDouble("balance")
                : 0;
        return Account.create(
                "Reject account - " + currency,
                Address.getDefaultInstance(),
                bic,
                format(numberFormat, currency),
                currency,
                balance);
    }
}

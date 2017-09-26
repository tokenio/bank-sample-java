package io.token.banksample.config;

import static java.util.stream.Collectors.toList;

import com.typesafe.config.Config;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.proto.common.pricing.PricingProtos.TransferQuote.FxRate;

import java.util.List;

/**
 * Parses configuration file and extracts different pieces of configuration.
 */
public final class ConfigParser {
    private final Config config;

    /**
     * Creates new configuration object.
     *
     * @param config config to parse
     */
    public ConfigParser(Config config) {
        this.config = config;
    }

    /**
     * Extracts list of accounts from the config.
     *
     * @return list of configured accounts
     */
    public List<AccountConfig> customerAccounts() {
        return accountsFor("customers");
    }

    /**
     * Extracts hold accounts list.
     *
     * @return hold accounts
     */
    public List<AccountConfig> holdAccounts() {
        return accountsFor("hold");
    }

    /**
     * Extracts settlement account list.
     *
     * @return settlement accounts
     */
    public List<AccountConfig> settlementAccounts() {
        return accountsFor("settlement");
    }

    /**
     * FX accounts account list.
     *
     * @return FX accounts
     */
    public List<AccountConfig> fxAccounts() {
        return accountsFor("fx");
    }

    /**
     * Reject accounts account list.
     *
     * @return reject accounts
     */
    public List<AccountConfig> rejectAccounts() {
        return accountsFor("reject");
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

    private List<AccountConfig> accountsFor(String category) {
        return config.getConfigList("accounts." + category)
                .stream()
                .map(c -> {
                    Address address = Address.getDefaultInstance();
                    if (c.hasPath("address")) {
                        Config addressConfig = c.getConfig("address");
                        address = Address.newBuilder()
                                .setHouseNumber(addressConfig.getString("house"))
                                .setStreet(addressConfig.getString("street"))
                                .setCity(addressConfig.getString("city"))
                                .setPostCode(addressConfig.getString("post_code"))
                                .setCountry(addressConfig.getString("country"))
                                .build();
                    }

                    double balance = c.hasPath("balance")
                            ? c.getDouble("balance")
                            : 0;

                    return AccountConfig.create(
                            c.getString("name"),
                            address,
                            c.getString("bic"),
                            c.getString("number"),
                            c.getString("currency"),
                            balance);
                })
                .collect(toList());
    }
}

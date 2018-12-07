package io.token.banksample.config;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.typesafe.config.Config;
import io.token.banksample.model.AccessTokenAuthorization;
import io.token.proto.common.account.AccountProtos.AccountFeatures;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.address.AddressProtos.Address;
import io.token.sdk.NamedAccount;
import io.token.security.SecretKeyStore;
import io.token.security.TrustedKeyStore;
import io.token.security.keystore.KeyStoreFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * FX accounts account list.
     *
     * @return FX accounts
     */
    public List<AccountConfig> fxAccounts() {
        return accountsFor("fx");
    }

    /**
     * Extracts bank id from config
     *
     * @return bank id
     */
    public String bankId() {
        return config.getString("bank.bank-id");
    }

    /**
     * Extracts the secret key store for generating bank authorization payload
     * for account linking.
     *
     * @return SecretKeyStore
     */
    public SecretKeyStore secretKeyStore() {
        return KeyStoreFactory.createSecretKeyStore(
                config.getConfigList("account-linking.secret-key-store"));
    }

    /**
     * Extracts the trusted key store for generating bank authorization payload
     * for account linking
     *
     * @return TrustedKeyStore
     */
    public TrustedKeyStore trustedKeyStore() {
        return KeyStoreFactory.createTrustedKeyStore(
                config.getConfigList("account-linking.trusted-key-store"));
    }

    /**
     * Extracts the id of the key to be used for encryption for account linking.
     *
     * @return encryption key id
     */
    public String encryptionKeyId() {
        return config.getString("account-linking.encryption.encryption-key-id");
    }

    /**
     * Extracts the encryption method for account linking.
     *
     * @return encryption method
     */
    public String encryptionMethod() {
        return config.getString("account-linking.encryption.encryption-method");
    }

    /**
     * Extracts map of access token string to access token authorization object.
     *
     * @return access token authorization map
     */
    public Map<String, AccessTokenAuthorization> accessTokenAuthorizations() {
        return config.getConfigList("access-tokens")
                .stream()
                .map(c -> {
                    List<NamedAccount> namedAccounts = c.getStringList("accounts")
                            .stream()
                            .map(number -> toNamedAccount(customerAccounts()
                                    .stream()
                                    .filter(acc -> acc.getNumber().equals(number))
                                    .collect(Collectors.reducing((a, b) -> null))
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Zero or multiple accounts match "
                                                    + "the account number "
                                                    + number))))
                            .collect(toList());
                    return AccessTokenAuthorization.create(
                            c.getString("access-token"),
                            c.getString("member-id"),
                            namedAccounts);
                })
                .collect(toMap(auth -> auth.accessToken(), auth -> auth));
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

    private NamedAccount toNamedAccount(AccountConfig accountConfig) {
        return new NamedAccount(
                BankAccount.newBuilder()
                        .setSwift(
                                BankAccount.Swift.newBuilder()
                                        .setAccount(accountConfig.getNumber())
                                        .setBic(accountConfig.getBic()))
                        .setAccountFeatures(AccountFeatures.newBuilder()
                                .setSupportsPayment(true)
                                .setSupportsReceivePayment(true)
                                .setSupportsSendPayment(true)
                                .setSupportsInformation(true))
                        .build(),
                accountConfig.getName());
    }
}

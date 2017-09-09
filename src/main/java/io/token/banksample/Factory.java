package io.token.banksample;

import com.typesafe.config.ConfigFactory;
import io.token.banksample.config.Configuration;
import io.token.banksample.impl.BankServiceImpl;
import io.token.banksample.impl.StorageServiceImpl;
import io.token.banksample.model.Accounts;
import io.token.banksample.model.Pricing;
import io.token.banksample.model.impl.AccountsImpl;
import io.token.banksample.model.impl.PricingImpl;
import io.token.sdk.api.BankService;
import io.token.sdk.api.StorageService;

import java.io.File;

/**
 * A factory class that is used to instantiate various services that are
 * exposed by the gRPC server.
 */
final class Factory {
    private final Configuration config;

    /**
     * Creates new factory instance.
     *
     * @param configFilePath path to the config directory
     */
    Factory(String configFilePath) {
        File configFile = new File(configFilePath);
        this.config = new Configuration(ConfigFactory.parseFile(configFile));
    }

    /**
     * Creates new {@link StorageService} instance.
     *
     * @return new storage service instance
     */
    StorageService storageService() {
        return new StorageServiceImpl();
    }

    /**
     * Creates new {@link BankService} instance.
     *
     * @return new bank service instance
     */
    BankService bankService() {
        Accounts accounts = new AccountsImpl(config.accountList());
        Pricing pricing = new PricingImpl(config.fxRateList(), config.transactionFee());
        return new BankServiceImpl(accounts, pricing);
    }
}

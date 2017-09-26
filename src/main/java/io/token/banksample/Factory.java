package io.token.banksample;

import static java.util.stream.Collectors.toList;

import com.typesafe.config.ConfigFactory;
import io.token.banksample.config.AccountConfig;
import io.token.banksample.config.ConfigParser;
import io.token.banksample.services.AccountServiceImpl;
import io.token.banksample.services.InstantTransferServiceImpl;
import io.token.banksample.services.PricingServiceImpl;
import io.token.banksample.services.StorageServiceImpl;
import io.token.banksample.services.TransferServiceImpl;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Accounts;
import io.token.banksample.model.Pricing;
import io.token.banksample.model.impl.AccountingImpl;
import io.token.banksample.model.impl.AccountsImpl;
import io.token.banksample.model.impl.PricingImpl;
import io.token.sdk.api.service.AccountService;
import io.token.sdk.api.service.InstantTransferService;
import io.token.sdk.api.service.PricingService;
import io.token.sdk.api.service.StorageService;
import io.token.sdk.api.service.TransferService;

import java.io.File;
import java.util.List;

/**
 * A factory class that is used to instantiate various services that are
 * exposed by the gRPC server.
 */
final class Factory {
    private final Accounting accounting;
    private final Pricing pricing;

    /**
     * Creates new factory instance.
     *
     * @param configFilePath path to the config directory
     */
    Factory(String configFilePath) {
        File configFile = new File(configFilePath);
        ConfigParser config = new ConfigParser(ConfigFactory.parseFile(configFile));

        List<String> currencies = config.customerAccounts().stream()
                .map(a -> a.getBalance().getCurrency())
                .distinct()
                .collect(toList());

        List<AccountConfig> holdAccounts = currencies.stream()
                .map(config::holdAccountFor)
                .collect(toList());
        List<AccountConfig> settlementAccounts = currencies.stream()
                .map(config::settlementAccountFor)
                .collect(toList());
        List<AccountConfig> fxAccounts = currencies.stream()
                .map(config::fxAccountFor)
                .collect(toList());
        List<AccountConfig> rejectAccounts = currencies.stream()
                .map(config::rejectAccountFor)
                .collect(toList());

        Accounts accounts = new AccountsImpl(
                holdAccounts,
                settlementAccounts,
                fxAccounts,
                rejectAccounts,
                config.customerAccounts());
        this.accounting = new AccountingImpl(accounts);
        this.pricing = new PricingImpl(config.fxRates());
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
     * Creates new {@link AccountService} instance.
     *
     * @return new account service instance
     */
    AccountService accountService() {
        return new AccountServiceImpl(accounting);
    }

    /**
     * Creates new {@link InstantTransferService} instance.
     *
     * @return new pricing service instance
     */
    PricingService pricingService() {
        return new PricingServiceImpl(accounting, pricing);
    }

    /**
     * Creates new {@link InstantTransferService} instance.
     *
     * @return new instant updatePayment service instance
     */
    InstantTransferService instantTransferService() {
        return new InstantTransferServiceImpl(accounting, pricing);
    }

    /**
     * Creates new {@link TransferService} instance.
     *
     * @return new updatePayment service instance
     */
    TransferService transferService() {
        return new TransferServiceImpl(accounting);
    }
}

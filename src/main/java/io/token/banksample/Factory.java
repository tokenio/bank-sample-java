package io.token.banksample;

import static java.util.stream.Collectors.toList;

import com.typesafe.config.ConfigFactory;
import io.token.banksample.config.Account;
import io.token.banksample.config.Configuration;
import io.token.banksample.impl.AccountServiceImpl;
import io.token.banksample.impl.InstantTransferServiceImpl;
import io.token.banksample.impl.PricingServiceImpl;
import io.token.banksample.impl.StorageServiceImpl;
import io.token.banksample.impl.TransferServiceImpl;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Pricing;
import io.token.banksample.model.impl.AccountingImpl;
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
    private final Configuration config;
    private final Accounting accounting;
    private final Pricing pricing;

    /**
     * Creates new factory instance.
     *
     * @param configFilePath path to the config directory
     */
    Factory(String configFilePath) {
        File configFile = new File(configFilePath);
        this.config = new Configuration(ConfigFactory.parseFile(configFile));

        List<Account> customerAccounts = config.accounts();
        List<Account> holdAccounts = customerAccounts.stream()
                .map(a -> a.getBalance().getCurrency())
                .distinct()
                .map(config::holdAccountFor)
                .collect(toList());
        List<Account> settlementAccounts = customerAccounts.stream()
                .map(a -> a.getBalance().getCurrency())
                .distinct()
                .map(config::settlementAccountFor)
                .collect(toList());
        List<Account> fxAccounts = customerAccounts.stream()
                .map(a -> a.getBalance().getCurrency())
                .distinct()
                .map(config::fxAccountFor)
                .collect(toList());
        this.accounting = new AccountingImpl(
                holdAccounts,
                settlementAccounts,
                fxAccounts,
                customerAccounts);
        this.pricing = new PricingImpl(config.fxRates(), config.transactionFee());
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
     * @return new instant transfer service instance
     */
    InstantTransferService instantTransferService() {
        return new InstantTransferServiceImpl(accounting, pricing);
    }

    /**
     * Creates new {@link TransferService} instance.
     *
     * @return new transfer service instance
     */
    TransferService transferService() {
        return new TransferServiceImpl();
    }
}

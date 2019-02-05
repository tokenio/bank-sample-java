package io.token.banksample;

import com.typesafe.config.ConfigFactory;
import io.token.banksample.config.ConfigParser;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Accounts;
import io.token.banksample.model.impl.AccountingImpl;
import io.token.banksample.model.impl.AccountsImpl;
import io.token.banksample.services.AccountLinkingServiceImpl;
import io.token.banksample.services.AccountServiceImpl;
import io.token.banksample.services.StorageServiceImpl;
import io.token.banksample.services.TransferServiceImpl;
import io.token.sdk.api.service.AccountLinkingService;
import io.token.sdk.api.service.AccountService;
import io.token.sdk.api.service.StorageService;
import io.token.sdk.api.service.TransferService;

import java.io.File;

public class Factory {
    private final Accounting accounting;
    private final ConfigParser config;

    /**
     * Creates new factory instance.
     *
     * @param configFilePath path to the config directory
     */
    public Factory(String configFilePath) {
        File configFile = new File(configFilePath);
        ConfigParser config = new ConfigParser(ConfigFactory.parseFile(configFile));
        Accounts accounts = new AccountsImpl(
                config.holdAccounts(),
                config.fxAccounts(),
                config.customerAccounts());

        this.accounting = new AccountingImpl(accounts);
        this.config = config;
    }

    /**
     * Creates new {@link StorageService} instance.
     *
     * @return new storage service instance
     */
    public StorageService storageService() {
        return new StorageServiceImpl();
    }

    /**
     * Creates new {@link AccountService} instance.
     *
     * @return new account service instance
     */
    public AccountService accountService() {
        return new AccountServiceImpl(accounting);
    }

    /**
     * Creates new {@link AccountLinkingService} instance.
     *
     * @return new account linking service instance
     */
    public AccountLinkingService accountLinkingService() {
        return new AccountLinkingServiceImpl(config);
    }

    /**
     * Creates new {@link TransferService} instance.
     *
     * @return new transfer service instance
     */
    public TransferService transferService() {
        return new TransferServiceImpl(accounting);
    }
}

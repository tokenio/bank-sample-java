package io.token.banksample;

import com.typesafe.config.ConfigFactory;
import io.token.banksample.config.ConfigParser;
import io.token.banksample.model.Accounting;
import io.token.banksample.model.Accounts;
import io.token.banksample.model.impl.AccountingImpl;
import io.token.banksample.model.impl.AccountsImpl;
import io.token.proto.common.security.SecurityProtos;
import io.token.sdk.BankAccountAuthorizer;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main service class. {@link CliArgs} defines the available command line arguments}.
 * The application parses command line arguments and then configures and starts
 * gRPC server that listens for incoming requests.
 */
public final class Application {
    private final static Logger logger = LoggerFactory.getLogger(Application.class);

    /**
     * App main entry.
     *
     * @param argv cli args
     */
    public static void main(String[] argv) {
        CliArgs args = CliArgs.parse(argv);
        logger.info("Command line arguments: {}", args);

        int port = args.port;
        String certFile = args.configPath("tls", "cert.pem");
        String keyFile = args.configPath("tls", "key.pem");
        String trustedCertFile = args.configPath("tls", "trusted-certs.pem");

        File configFile = new File(args.configPath("application.conf"));
        ConfigParser config = new ConfigParser(ConfigFactory.parseFile(configFile));
        Accounts accounts = new AccountsImpl(
                config.holdAccounts(),
                config.fxAccounts(),
                config.customerAccounts());

        // Accounting service: Entry point to the Ruby Bank API
        Accounting accounting = new AccountingImpl(accounts);

        // Bank Account Authorizer: used to construct Bank Authorization payload
        BankAccountAuthorizer authorizer = BankAccountAuthorizer.builder(config.bankId())
                .withSecretKeystore(config.secretKeyStore())
                .withTrustedKeystore(config.trustedKeyStore())
                .useKey(config.encryptionKeyId())
                .useMethod(SecurityProtos.SealedMessage.MethodCase.valueOf(
                        config.encryptionMethod()))
                // expiration is set to 1 day by default
                .build();

        // TODO: Use ServerBuilder to create a gRPC server, exposing the Bank API endpoints.
    }
}

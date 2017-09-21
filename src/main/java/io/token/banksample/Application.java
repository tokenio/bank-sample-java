package io.token.banksample;

import io.token.sdk.ServerBuilder;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main service class. {@link CliArgs defines the available command line arguments}.
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
    public static void main(String[] argv) throws IOException, InterruptedException {
        CliArgs args = CliArgs.parse(argv);
        logger.info("Command line arguments: {}", args);

        // Create a factory used to instantiate all the service implementations
        // that are needed to initialize the server.
        Factory factory = new Factory(args.configPath("application.conf"));

        // Build a gRPC server instance.
        ServerBuilder server = ServerBuilder
                .forPort(args.port)
                .reportErrorDetails()
                .withAccountService(factory.accountService())
                .withInstantTransferService(factory.instantTransferService())
                .withTransferService(factory.transferService())
                .withPricingService(factory.pricingService())
                .withStorageService(factory.storageService());
        if (args.useSsl) {
                server.withTls(
                    args.configPath("tls", "cert.pem"),
                    args.configPath("tls", "key.pem"),
                    args.configPath("tls", "trusted-certs.pem"));
        }

        // You will need to Ctrl-C to exit.
        server
                .build()
                .start()
                .await();
    }
}

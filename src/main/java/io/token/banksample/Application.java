package io.token.banksample;

import io.token.sdk.HttpServerBuilder;
import io.token.sdk.ServerBuilder;

import java.io.IOException;

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
    public static void main(String[] argv) throws IOException, InterruptedException {
        CliArgs args = CliArgs.parse(argv);
        logger.info("Command line arguments: {}", args);

        if (args.useHttp) {
            startHttpServer(args);
        } else {
            startRpcServer(args);
        }
    }

    private static void startRpcServer(CliArgs args) {
        // Create a factory used to instantiate all the service implementations
        // that are needed to initialize the server.
        Factory factory = new Factory(args.configPath("application.conf"));

        // Build a gRPC server instance.
        ServerBuilder server = ServerBuilder
                .forPort(args.port)
                .reportErrorDetails()
                .withAccountService(factory.accountService())
                .withAccountLinkingService(factory.accountLinkingService())
                .withTransferService(factory.transferService())
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

    private static void startHttpServer(CliArgs args) {
        // Create a factory used to instantiate all the service implementations
        // that are needed to initialize the server.
        Factory factory = new Factory(args.configPath("application.conf"));

        // Build an HTTP server instance.
        HttpServerBuilder server = HttpServerBuilder
                .forPort(args.port)
                .reportErrorDetails()
                .withAccountService(factory.accountService())
                .withAccountLinkingService(factory.accountLinkingService())
                .withTransferService(factory.transferService())
                .withStorageService(factory.storageService());

        if (args.httpBearerToken != null) {
            server.withBearerAuthorization(args.httpBearerToken);
        }

        // You will need to Ctrl-C to exit.
        server
                .build()
                .start();
    }
}

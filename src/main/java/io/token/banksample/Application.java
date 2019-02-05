package io.token.banksample;

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

        // Create a factory used to instantiate all the service implementations
        // that are needed to initialize the server.
        Factory factory = new Factory(args.configPath("application.conf"));
    }
}

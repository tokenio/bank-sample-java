package io.token.banksample;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.token.banksample.impl.BankServiceImpl;
import io.token.banksample.impl.StorageServiceImpl;
import io.token.rpc.server.RpcServer;
import io.token.sdk.ServerBuilder;
import io.token.sdk.api.BankService;
import io.token.sdk.api.StorageService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main service class. {@link Args defines the available command line arguments}.
 */
public final class Application {
    private final static Logger logger = LoggerFactory.getLogger(Application.class);

    public static class Args {
        @Parameter(names = { "--port", "-p" }, description = "gRPC port to listen on")
        private int port = 9300;

        @Parameter(names = { "--config", "-c" }, description = "Config directory location")
        private Path config = new File("config").toPath();

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, NO_CLASS_NAME_STYLE);
        }
    }

    /**
     * App main entry.
     *
     * @param argv cli args
     */
    public static void main(String[] argv) throws IOException, InterruptedException {
        // Parse command line arguments.
        Args args = new Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(argv);
        logger.info("Command line arguments: {}", args);

        // Create implementations of the required services.
        BankService bankService = new BankServiceImpl();
        StorageService storageService = new StorageServiceImpl();

        // Build a gRPC server instance.
        RpcServer server = ServerBuilder
                .forPort(args.port)
                .withTls(
                        configPath(args.config, "tls", "cert.pem"),
                        configPath(args.config, "tls", "key.pem"),
                        configPath(args.config, "tls", "trusted-certs.pem"))
                .reportErrorDetails()
                .withBankService(bankService)
                .withStorageService(storageService)
                .build();

        // You will need to Ctrl-C to exit.
        server
                .start()
                .await();
    }

    private static String configPath(Path root, String ... segments) {
        Path path = root;
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toString();
    }
}

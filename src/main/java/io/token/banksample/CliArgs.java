package io.token.banksample;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * Command line arguments supported by the server application.
 */
public class CliArgs {
    @Parameter(names = { "--port", "-p" }, description = "gRPC port to listen on")
    int port = 9300;

    @Parameter(names = { "--ssl", "-s" }, description = "Use SSL")
    boolean useSsl = false;

    @Parameter(names = { "--config", "-c" }, description = "Config directory location")
    private Path config = new File("config").toPath();

    @Parameter(names = { "--usage", "-u" }, description = "Show usage")
    private boolean usage;

    @Parameter(names = { "--http" }, description = "Use HTTP")
    boolean useHttp = false;

    @Parameter(names = { "--http-bearer-token" }, description = "Set HTTP Bearer token")
    String httpBearerToken;

    private CliArgs() {}

    /**
     * Parses out command line arguments.
     *
     * @param argv passed in arguments
     * @return parsed arguments
     */
    static CliArgs parse(String[] argv) {
        CliArgs args = new CliArgs();

        JCommander jCommander = new JCommander(args);
        jCommander.parse(argv);

        if (args.usage) {
            jCommander.usage();
            System.exit(0);
        }

        return args;
    }

    /**
     * Combines config directory and provided segments into a single file
     * path. E.g. given "." as the config root and "config" and "file.txt"
     * as path segments it produces result of "./config/file.txt".
     *
     * @param segments file path segments
     * @return full combined path
     */
    String configPath(String ... segments) {
        Path path = config;
        for (String segment : segments) {
            path = path.resolve(segment);
        }
        return path.toString();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, NO_CLASS_NAME_STYLE);
    }
}

package com.example.demo;

import com.example.demo.impl.BankServiceImpl;
import com.example.demo.impl.StorageServiceImpl;
import io.token.rpc.server.RpcServer;
import io.token.sdk.ServerBuilder;
import io.token.sdk.api.BankService;
import io.token.sdk.api.StorageService;

import java.io.IOException;

public class Application {
    /**
     * App main entry.
     *
     * @param args cli args
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // Configuration parameters. The certs and keys are in the ./config directory.
        int port = 9300;
        String certFile = "config/cert.pem";
        String keyFile = "config/key.pem";
        String trustedCertFile = "config/trusted-certs.pem";

        // Create implementations of the required services.
        BankService bankService = new BankServiceImpl();
        StorageService storageService = new StorageServiceImpl();

        // Build a gRPC server instance.
        RpcServer server = ServerBuilder
                .forPort(port)
                .withTls(certFile, keyFile, trustedCertFile)
                .reportErrorDetails()
                .withBankService(bankService)
                .withStorageService(storageService)
                .build();

        // You will need to Ctrl-C to exit.
        server
                .start()
                .await();
    }
}

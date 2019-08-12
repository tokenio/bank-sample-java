package io.token.banksample;

import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.security.crypto.CryptoType;

import java.security.KeyPair;

import org.junit.Test;

public class GenerateKeyTest {
    @Test
    public void generate() {
        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(CryptoType.EDDSA);
        KeyPair keyPair = crypto.generateKeyPair();
        System.out.println("crypto: " + crypto.getAlgorithm().toUpperCase());
        System.out.println("private-key (Used for signing bank auth payloads):"
                + crypto.serialize(keyPair.getPrivate()));
        System.out.println("public-key (Give to Token so that Token can verify bank auth payloads):"
                + crypto.serialize(keyPair.getPublic()));
    }
}

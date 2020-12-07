package io.token.banksample;


import com.google.common.hash.Hashing;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.security.crypto.CryptoType;

import java.security.KeyPair;

import io.token.util.codec.ByteEncoding;
import org.junit.Test;

public class GenerateKeyTest {
    @Test
    public void generate() {
        Crypto crypto = CryptoRegistry.getInstance().cryptoFor(CryptoType.EDDSA);
        KeyPair keyPair = crypto.generateKeyPair();
        System.out.println("crypto: " + crypto.getAlgorithm().toUpperCase());
        System.out.println("private-key: "
                + crypto.serialize(keyPair.getPrivate())
                + " // Used for signing bank auth payloads");
        System.out.println("public-key: "
                + crypto.serialize(keyPair.getPublic())
                + "  // Give to Token so that Token can verify bank auth payloads");

        System.out.println("Key-ID for public key: " +keyIdFor(crypto.serialize(keyPair.getPublic())));
        System.out.println("Key-ID for already generated public key: " +keyIdFor("Enter Your Key ID")); //Incase Public Key Already Generated
    }


    static String keyIdFor(String serializedKey) {
        byte[] encoded = ByteEncoding.parse(serializedKey);
        byte[] hash = Hashing.sha256().newHasher()
                .putBytes(encoded)
                .hash()
                .asBytes();
        return ByteEncoding.serialize(hash).substring(0, 16);
    }


}


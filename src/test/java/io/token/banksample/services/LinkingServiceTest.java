package io.token.banksample.services;

import static io.token.proto.ProtoJson.fromJson;
import static io.token.security.crypto.CryptoType.EDDSA;
import static io.token.security.crypto.CryptoType.RSA_SHA1;
import static org.assertj.core.api.Assertions.assertThat;

import io.token.banksample.Factory;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.proto.common.account.AccountProtos;
import io.token.proto.common.account.AccountProtos.BankAccount;
import io.token.proto.common.account.AccountProtos.PlaintextBankAuthorization;
import io.token.proto.common.security.SecurityProtos.SealedMessage;
import io.token.sdk.api.service.AccountLinkingService;
import io.token.security.crypto.Crypto;
import io.token.security.crypto.CryptoRegistry;
import io.token.security.crypto.CryptoType;
import io.token.security.keystore.InMemorySecretKeyStore;
import io.token.security.keystore.InMemoryTrustedKeyStore;
import io.token.security.keystore.SecretKeyPair;
import io.token.security.keystore.TrustedKey;
import io.token.security.sealed.SealedMessageDecrypter;

import java.security.KeyPair;
import java.security.PublicKey;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class LinkingServiceTest {
    private AccountLinkingService linkingService;
    private SealedMessageDecrypter sealedMessageDecrypter;

    @Before
    public void before() {
        // Token's secret key
        String rsaPub = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgZHA2uQMt_iKXi3AXYIPxyjVDCydV4JW6zl1I12gEWtMyiWRZx9djgPaHLx-o-hgGm9lKinfrbyV0nC63Wc2O-161eDma-d-RcblcktpTGHAlfsjXU8UlsmMj_ItjH4TN-aWXjs1g-o0-2F3nbYE_UofRDqqlQPED8NSXADrazH0MA8QuuVR3AuBl1NnnKHqlnUw4zvwUAYxfqUTnZPQMTebGP2XsdT5LYOIrswepjdV9BGQf_rmDq_ZmTdaM05qQgWqYG97xPVNbKhpZDahIzVU-TuKC1ez68JVS-NWko5b2bugBp3mdnkVpAI7shmOrDFQZY76Cch1aZBAEjGCSQIDAQAB";
        String rsaPriv = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCBkcDa5Ay3-IpeLcBdgg_HKNUMLJ1XglbrOXUjXaARa0zKJZFnH12OA9ocvH6j6GAab2UqKd-tvJXScLrdZzY77XrV4OZr535FxuVyS2lMYcCV-yNdTxSWyYyP8i2MfhM35pZeOzWD6jT7YXedtgT9Sh9EOqqVA8QPw1JcAOtrMfQwDxC65VHcC4GXU2ecoeqWdTDjO_BQBjF-pROdk9AxN5sY_Zex1Pktg4iuzB6mN1X0EZB_-uYOr9mZN1ozTmpCBapgb3vE9U1sqGlkNqEjNVT5O4oLV7PrwlVL41aSjlvZu6AGneZ2eRWkAjuyGY6sMVBljvoJyHVpkEASMYJJAgMBAAECggEAGP9p2dFNsuC8sVbaWjARozb5g5PH924qHs_DDcOuci3lbsq4ttCSWCfeGNU1CaJ3iCIdvni9suNDdIpTQwv6pq02mbT-P6s17XhmJBrwgdAKO-Vr-UCclErmV489wnFAe_R85kk-FelFt4oibccER2nZhmxbJMzJMtFYPm_e-5stSF1kBg2oTjorrjrAnVLnjinnnPMPZW6EjUC-AmvBGgOhVW8799yhqIWREO3ETIZ0m-IKy5BfJzXgfNSJcS3Ua4YlyMw9QT0Uka60nt-tPxmXUPT4aJsp6DXh-vSyI1IaUsdq8zZJkK50xcP3XkOdqchN0XM6sd2Kav56fhgeQQKBgQC_c-7ukY_hXk4W26rvQldm0KZIRw4vIomjCqg-4ZTbw1G3s9RAG9R767fQRalqJhHarTVSr0iZpbHDD_Al3MO5zwWiNfhQ7l6hCWp4vED_wjxSQBpjjptgaN2tEBtCokjvE8P3226SemBe6GjKSu4sLJ0s9ybOmcEDA9ya0MhFrQKBgQCtQLpn5w1k6ZCC8KfDwAVAxlh4OSiQfpjH4KVhQ4hRh1AoDPSCm71muCIurqVkrpPDTtMJJCFG1Jp658TuaVhx5lrKt9Pv2Ymtyhwz7KFmxNpt-rkipiDxNKn6axrTEU8cLGF40atNsErQuaQHgf8q1PrrRZF4WmaZ07617V_qjQKBgBCOEtQYTCu9vtzlI9PKlcozMp2_Xy-eOe6aRlAhq4CRVCihaTId9fK9QEjHLU4beqHBJoI-2_VFSajHYQm1HEud5ivPpOhCpHYiXU2RYcF750Fpf56qPy9IAhsr8F-DrvDVcbWmCNqrsFekmyMa9ZtmRDUWLgnha7o6BEJy2U6FAoGBAJtkjAy3Wvcg2mHnMcHMD6oafK1QLwby1VupLQxBAxqE8CsL-57NyyI_k0ElmejlAFlT8MqpIkOiWvBTwvlHeXGnd9WM7cWZ6vf6aDNWUSmY9IViNP1T3gYaAz89I3EefEM7ty0jea8TqPbvGqU1PsWPA4NUAXbeNlpQZiRMxRPdAoGBAJfWV5D5UpSW6GuHFe8jl0QPi23KPwMxmQWXDP7KCo16dfLtwrNhgQgaFBp6cY-ts_SEbwQLujwmOXcl8xtwu_w_bzQ3mOJRCfPRd__ktbnB2k1lrA5zFAyri39KHN_I-8KGiwtiLySjE7amN4vzasPvSGYr8F7Aj_GePZ25RUa-";

        // trusted bank key
        String eddsaPub = "0I3gkpgFCEzRSxtPX3_vTd7Ct7bFVGhpB5tC9YuCmNA";

        Factory factory = new Factory("config/application.conf");
        linkingService = factory.accountLinkingService();

        Crypto rsaCrypto = CryptoRegistry.getInstance()
                .cryptoFor(CryptoType.RS256);

        Crypto eddsaCrypto = CryptoRegistry.getInstance()
                .cryptoFor(CryptoType.EDDSA);

        sealedMessageDecrypter = new SealedMessageDecrypter(
                new InMemorySecretKeyStore(SecretKeyPair.create(true, RSA_SHA1, new KeyPair(rsaCrypto.toPublicKey(rsaPub), rsaCrypto.toPrivateKey(rsaPriv)))),
                new InMemoryTrustedKeyStore(TrustedKey.create(CryptoType.EDDSA, eddsaCrypto.toPublicKey(eddsaPub))));
    }

    @Test
    public void getBankAuthorization() {
        BankAuthorization bankAuthorization = linkingService.getBankAuthorization("R0RUAX0C1T");

        assertThat(bankAuthorization.getBankId()).isEqualTo("ruby");
        BankAccount result = decrypt(bankAuthorization.getAccounts(0)).getAccount();
        assertThat(result.hasSwift());
        assertThat(result.getSwift().getBic()).isEqualTo("RUBYUSCA000");
        assertThat(result.getSwift().getAccount()).isEqualTo("0000001");
    }

    private PlaintextBankAuthorization decrypt(SealedMessage message) {
        return fromJson(
                sealedMessageDecrypter.decrypt(message),
                PlaintextBankAuthorization.newBuilder());
    }
}

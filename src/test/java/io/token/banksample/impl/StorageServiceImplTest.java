package io.token.banksample.impl;

import static org.assertj.core.api.Assertions.assertThat;

import io.token.proto.bankapi.Bankapi.SetValueRequest.ContentCategory;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

public class StorageServiceImplTest {
    private StorageServiceImpl storageService;

    @Before
    public void before() {
        storageService = new StorageServiceImpl();
    }

    @Test
    public void storeAndRetrieve() {
        {
            String key = RandomStringUtils.randomAlphanumeric(20);
            byte[] value = RandomUtils.nextBytes(100);

            storageService.setValue(key, ContentCategory.ACCOUNT_INFO, value);
            Optional<byte[]> got = storageService.getValue(key);
            assertThat(got.isPresent()).isTrue();
            assertThat(got.get()).isEqualTo(value);
        }

        {
            String key = RandomStringUtils.randomAlphanumeric(20);
            byte[] value = RandomUtils.nextBytes(100);

            storageService.setValue(key, ContentCategory.TOKEN_INFO, value);
            Optional<byte[]> got = storageService.getValue(key);
            assertThat(got.isPresent()).isTrue();
            assertThat(got.get()).isEqualTo(value);
        }
    }

    @Test
    public void storeThenClearGetNothing() {
        String key = RandomStringUtils.randomAlphanumeric(20);

        storageService.setValue(key, ContentCategory.ACCOUNT_INFO, RandomUtils.nextBytes(100));
        assertThat(storageService.getValue(key).isPresent()).isTrue();

        storageService.removeValue(key);
        assertThat(storageService.getValue(key).isPresent()).isFalse();
    }
}

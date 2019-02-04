package io.token.banksample.services;

import io.token.proto.bankapi.Bankapi.SetValueRequest.ContentCategory;
import io.token.sdk.api.service.StorageService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of the {@link StorageService}. This needs to be
 * backed by a durable store to work in production.
 */
public class StorageServiceImpl implements StorageService {
    private final Map<String, byte[]> storage = new HashMap<>();

    @Override
    public synchronized Optional<byte[]> getValue(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    @Override
    public synchronized Optional<byte[]> setValue(
            String key,
            ContentCategory category,
            byte[] value) {
        return Optional.ofNullable(storage.put(key, value));
    }

    @Override
    public synchronized void removeValue(String key) {
        storage.remove(key);
    }
}

package io.token.banksample.services;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.banksample.config.ConfigParser;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.api.service.AccountLinkingService;

public class AccountLinkingServiceImpl implements AccountLinkingService {
    private final ConfigParser config;

    public AccountLinkingServiceImpl(ConfigParser config) {
        this.config = config;
    }

    @Override
    public BankAuthorization getBankAuthorization(String accessToken) {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }
}

package io.token.banksample.services;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.banksample.model.AccessTokenAuthorization;
import io.token.proto.banklink.Banklink.BankAuthorization;
import io.token.sdk.BankAccountAuthorizer;
import io.token.sdk.api.service.AccountLinkingService;

import java.util.Map;

public class AccountLinkingServiceImpl implements AccountLinkingService {
    private final Map<String, AccessTokenAuthorization> authorizations;
    private final BankAccountAuthorizer authorizer;

    public AccountLinkingServiceImpl(
            Map<String, AccessTokenAuthorization> authorizations,
            BankAccountAuthorizer authorizer) {
        this.authorizations = authorizations;
        this.authorizer = authorizer;
    }

    @Override
    public BankAuthorization getBankAuthorization(String accessToken) {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }
}

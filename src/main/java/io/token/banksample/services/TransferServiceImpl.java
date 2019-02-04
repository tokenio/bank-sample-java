package io.token.banksample.services;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.token.banksample.model.Accounting;
import io.token.sdk.api.Transfer;
import io.token.sdk.api.TransferException;
import io.token.sdk.api.service.TransferService;

/**
 * Sample implementation of the {@link TransferService}. Returns fake data.
 */
public class TransferServiceImpl implements TransferService {
    private final Accounting accounts;

    public TransferServiceImpl(Accounting accounts) {
        this.accounts = accounts;
    }

    @Override
    public String transfer(Transfer transfer) throws TransferException {
        throw new StatusRuntimeException(Status.UNIMPLEMENTED);
    }
}

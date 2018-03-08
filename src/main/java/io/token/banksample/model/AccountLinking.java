package io.token.banksample.model;

import io.token.proto.banklink.Banklink.BankAuthorization;

public interface AccountLinking {
    BankAuthorization getBankAuthorization(String accessToken);
}

package io.token.banksample.model;

import com.google.auto.value.AutoValue;
import io.token.sdk.NamedAccount;

import java.util.List;

@AutoValue
public abstract class AccessTokenAuthorization {
    /**
     * Creates a new access token authorization object.
     *
     * @param accessToken access token string
     * @param memberId token member id
     * @param accounts list of named account
     * @return access token authorization object
     */
    public static AccessTokenAuthorization create(
            String accessToken,
            String memberId,
            List<NamedAccount> accounts) {
        return new AutoValue_AccessTokenAuthorization(
                accessToken,
                memberId,
                accounts);
    }

    public abstract String accessToken();

    public abstract String memberId();

    public abstract List<NamedAccount> accounts();
}

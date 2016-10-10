package org.zalando.undertaking.oauth2;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

public class AuthenticationInfo {

    private final Optional<String> uid;
    private final Set<String> scopes;
    private final Optional<String> businessPartnerId;

    public AuthenticationInfo(final Optional<String> uid, final Set<String> scopes,
            final Optional<String> businessPartnerId) {
        this.uid = requireNonNull(uid);
        this.scopes = ImmutableSet.copyOf(scopes);
        this.businessPartnerId = requireNonNull(businessPartnerId);
    }

    @Override
    public String toString() {
        return
            MoreObjects.toStringHelper(this)                                     //
                       .omitNullValues()                                         //
                       .add("uid", uid.orElse(null))                             //
                       .add("scopes", scopes)                                    //
                       .add("businessPartnerId", businessPartnerId.orElse(null)) //
                       .toString();
    }

    public Optional<String> getUid() {
        return uid;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Optional<String> getBusinessPartnerId() {
        return businessPartnerId;
    }
}

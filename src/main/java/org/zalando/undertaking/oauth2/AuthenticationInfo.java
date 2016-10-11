package org.zalando.undertaking.oauth2;

import java.util.Optional;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

public class AuthenticationInfo {

    private final Optional<String> uid;
    private final Set<String> scopes;
    private final Optional<String> businessPartnerId;

    public static class Builder {
        private String uid;
        private Set<String> scopes = ImmutableSet.of();
        private String businessPartnerId;

        protected Builder() {
            // use factory methods
        }

        public Builder uid(final String uid) {
            this.uid = uid;
            return this;
        }

        public Builder scopes(final Set<String> scopes) {
            this.scopes = ImmutableSet.copyOf(scopes);
            return this;
        }

        public Builder scopes(final String... scopes) {
            this.scopes = ImmutableSet.copyOf(scopes);
            return this;
        }

        public Builder businessPartnerId(final String businessPartnerId) {
            this.businessPartnerId = businessPartnerId;
            return this;
        }

        public AuthenticationInfo build() {
            return new AuthenticationInfo(this);
        }
    }

    protected AuthenticationInfo(final Builder builder) {
        uid = Optional.ofNullable(builder.uid);
        scopes = builder.scopes;
        businessPartnerId = Optional.ofNullable(builder.businessPartnerId);
    }

    public static Builder builder() {
        return new Builder();
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

    public final Optional<String> getUid() {
        return uid;
    }

    public final Set<String> getScopes() {
        return scopes;
    }

    public final Optional<String> getBusinessPartnerId() {
        return businessPartnerId;
    }

    public Builder with() {
        return newBuilder().uid(uid.orElse(null)) //
                           .scopes(scopes)        //
                           .businessPartnerId(businessPartnerId.orElse(null));
    }

    protected Builder newBuilder() {
        return builder();
    }
}

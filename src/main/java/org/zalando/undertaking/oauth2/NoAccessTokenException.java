package org.zalando.undertaking.oauth2;

public class NoAccessTokenException extends Exception {

    @Override
    public String getMessage() {
        return "No access token provided.";
    }

}

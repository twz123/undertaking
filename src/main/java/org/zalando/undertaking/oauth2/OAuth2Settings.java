package org.zalando.undertaking.oauth2;

/**
 * Provides configuration settings for the OAuth2 functionality.
 *
 * @deprecated  Use {@link AccessTokenSettings} and {@link AuthenticationInfoSettings} directly. This avoids having to
 *              implement methods, which are not required when only one of the two settings are required. This class is
 *              scheduled for removal in Undertaking 0.1.0.
 */
@Deprecated
public interface OAuth2Settings extends AccessTokenSettings, AuthenticationInfoSettings { }

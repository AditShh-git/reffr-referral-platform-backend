package com.Reffr_Backend.common.constants;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String BEARER_PREFIX         = "Bearer ";
    public static final String AUTHORIZATION_HEADER  = "Authorization";

    // Redis key prefixes
    public static final String REFRESH_TOKEN_PREFIX  = "session:";   // session:{userId}:{deviceId}
    public static final String TOKEN_DENYLIST_PREFIX = "denylist:";  // denylist:{tokenHash}

    // Cache names — single source of truth, matches RedisConfig
    public static final String CACHE_PUBLIC_PROFILE  = "publicProfile";
    public static final String CACHE_FEED            = "feed";
    public static final String CACHE_REFERRERS       = "referrers";
    public static final String CACHE_NOTIFICATIONS   = "notifications";

    // JWT claim keys
    public static final String CLAIM_USERNAME        = "username";
    public static final String CLAIM_TOKEN_TYPE      = "type";
    public static final String CLAIM_DEVICE_ID       = "deviceId";
}

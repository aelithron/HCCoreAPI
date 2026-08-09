package com.hackclub.hccoreapi;

import com.hackclub.hccoreapi.DataTypes.APIAccess;
import com.hackclub.hccoreapi.DataTypes.RateLimitInfo;

import java.util.HashMap;
import java.util.Map;

public class RateLimitManager {
    private final Map<String, RateLimitInfo> rateLimits = new HashMap<>();
    public boolean isRateLimited(APIAccess access) {
        RateLimitInfo info = rateLimits.get(access.id);
        if (info == null) {
            return false;
        }
        if (info.getExpiry() < System.currentTimeMillis()) {
            rateLimits.remove(access.id);
            return false;
        }
        return info.getCount() >= access.rateLimit;
    }
    public void countRateLimit(APIAccess access) {
        RateLimitInfo info = rateLimits.get(access.id);
        if (info == null) {
            rateLimits.put(access.id, new RateLimitInfo(1));
            return;
        }
        info.incrementCount();
    }
}

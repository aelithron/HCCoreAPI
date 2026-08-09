package com.hackclub.hccoreapi.DataTypes;

import com.hackclub.hccoreapi.KeyManager;

public class APIAccess {
    public String id;
    public String key;
    public int rateLimit;
    public boolean enabled;
    private final KeyManager mgr;
    public APIAccess(String id, String key, int rateLimit, boolean enabled, KeyManager mgr) {
        this.id = id;
        this.key = key;
        this.rateLimit = rateLimit;
        this.enabled = enabled;
        this.mgr = mgr;
    }
    public boolean validate() {
        return mgr.isKeyValid(this);
    }
}

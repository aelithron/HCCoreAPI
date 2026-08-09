package com.hackclub.hccoreapi.DataTypes;

public class RateLimitInfo {
    private int count; // to make it so i don't forget and add to it manually
    private final long expiry;
    public RateLimitInfo(int count) {
        this.count = count;
        this.expiry = System.currentTimeMillis() + 60_000L;
    }
    public int getCount() {
        return count;
    }
    public void incrementCount() {
        count++;
    }
    public long getExpiry() {
        return expiry;
    }
}

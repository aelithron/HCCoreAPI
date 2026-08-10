package com.hackclub.hccoreapi.DataTypes;

public class APIHealth {
    public String status = "ok";
    public String version;
    public Boolean authorized;
    public APIHealth(String version, Boolean authorized) {
        this.version = version;
        this.authorized = authorized;
    }
}

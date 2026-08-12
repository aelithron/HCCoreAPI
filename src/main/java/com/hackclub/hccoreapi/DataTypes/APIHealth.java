package com.hackclub.hccoreapi.DataTypes;

import com.fasterxml.jackson.annotation.JsonProperty;

public class APIHealth {
    @JsonProperty
    public String status = "ok";
    @JsonProperty
    public String version;
    @JsonProperty
    public Boolean authorized;

    public APIHealth(String version, Boolean authorized) {
        this.version = version;
        this.authorized = authorized;
    }
}

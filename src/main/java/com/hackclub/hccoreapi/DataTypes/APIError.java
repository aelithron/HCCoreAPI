package com.hackclub.hccoreapi.DataTypes;

public class APIError {
    public String error;
    public String message;
    public APIError(String error, String message) {
        this.error = error;
        this.message = message;
    }
}

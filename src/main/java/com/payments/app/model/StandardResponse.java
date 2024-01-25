package com.payments.app.model;

import com.google.gson.JsonElement;

public record StandardResponse(String status, String message, JsonElement data) {

    public StandardResponse(String name, String string) {
        this(name, string, null);
    }
} 
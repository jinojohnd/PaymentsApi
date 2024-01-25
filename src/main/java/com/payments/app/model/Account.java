package com.payments.app.model;

public record Account(String accountId, String currency, Double balance) {
    public Account(String accountId, Double balance) {
        this(accountId, "USD", balance);
    }

    public Account withBalance(Double newBalance) {
        return new Account(accountId, currency, newBalance);
    }
}

package com.payments.app.model;

import java.util.List;

public record User(String id, List<Account> accounts) {
}
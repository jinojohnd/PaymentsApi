package com.payments.app.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.payments.app.PaymentsApp;
import com.payments.app.constants.HttpMethod;
import com.payments.app.constants.HttpStatus;
import com.payments.app.model.Account;
import com.payments.app.utils.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StatementHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = processRequest(exchange);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String processRequest(HttpExchange exchange) throws IOException {
        if (!HttpMethod.GET.name().equalsIgnoreCase(exchange.getRequestMethod())) {
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_BAD_METHOD, HttpStatus.ERROR.name(),
                    "Invalid Method");
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String[] pathParams = exchange.getRequestURI().getPath().split("/");

        if (pathParams == null || pathParams.length != 4) {
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, HttpStatus.ERROR.name(),
                    "Invalid Request");
        }

        String userId = pathParams[3];
        List<Account> userAccounts = PaymentsApp.getUserAccountsMap().get(userId);

        if (userAccounts == null) {
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, HttpStatus.ERROR.name(),
                    "Invalid User");
        }

        return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_OK, HttpStatus.SUCCESS.name(), null,
                userAccounts);
    }
}

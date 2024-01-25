package com.payments.app.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.payments.app.PaymentsApp;
import com.payments.app.constants.HttpMethod;
import com.payments.app.constants.HttpStatus;
import com.payments.app.model.Account;
import com.payments.app.service.AccountOperationsService;
import com.payments.app.service.impl.AccountOperationsServiceImpl;
import com.payments.app.utils.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ExternalWithdrawalStatusHandler implements HttpHandler {

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
        String queryParam = exchange.getRequestURI().getQuery();

        Map<String, String> queryMap = HttpUtils.getFormDataParams(queryParam);

        if (!HttpUtils.containsKeys(queryMap, "withdrawalId")) {
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, HttpStatus.ERROR.name(),
                    "Invalid Request");
        }

        AccountOperationsService accService = new AccountOperationsServiceImpl();
        String state;
        try {
            state = "Transaction Status: " + accService.getRequestState(queryMap.get("withdrawalId"));
        } catch (Exception e) {
            System.err.println("Error " + e.getMessage());
            state = "Invalid Withdrawal Id";
        }
        return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_OK, HttpStatus.SUCCESS.name(), state, null
                );
    }
}

package com.payments.app.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.payments.app.constants.HttpMethod;
import com.payments.app.constants.HttpStatus;
import com.payments.app.model.StandardResponse;
import com.payments.app.service.AccountOperationsService;
import com.payments.app.service.impl.AccountOperationsServiceImpl;
import com.payments.app.utils.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AmountTransferHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = processRequest(exchange);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String processRequest(HttpExchange exchange) throws IOException {
        if (!HttpMethod.POST.name().equalsIgnoreCase(exchange.getRequestMethod())) {
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_BAD_METHOD, HttpStatus.ERROR.name(),
                    "Invalid Method");
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        String[] pathParams = exchange.getRequestURI().getPath().split("/");
        String requestBodyStr = HttpUtils.getRequestBody(exchange.getRequestBody());
        Map<String, String> formDataMap = HttpUtils.getFormDataParams(requestBodyStr);

        if (pathParams == null || pathParams.length != 4 || !HttpUtils.containsKeys(formDataMap, "beneAccountId", "ownAccountId", "amount")) {
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_NOT_FOUND, HttpStatus.ERROR.name(),
                    "Invalid Request");
        }

        String userId = pathParams[3];
        AccountOperationsService accService = new AccountOperationsServiceImpl();

        try {
            Map<Integer, String> apiResponse = accService.deposit(formDataMap.get("ownAccountId"),
                    formDataMap.get("beneAccountId"), userId, Double.parseDouble(formDataMap.get("amount")));

            return apiResponse.containsKey(1)
                    ? HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_OK,
                            new StandardResponse(HttpStatus.SUCCESS.name(), apiResponse.get(1)))
                    : HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_OK,
                            new StandardResponse(HttpStatus.ERROR.name(), apiResponse.get(0)));

        } catch (Exception e) {
            System.err.println("Error " + e);
            return HttpUtils.getResponse(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, HttpStatus.ERROR.name(),
                    "Failed to process the transaction");
        }
    }
}

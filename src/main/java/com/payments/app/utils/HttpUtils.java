package com.payments.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.RuntimeErrorException;

import com.google.gson.Gson;
import com.payments.app.constants.HttpStatus;
import com.payments.app.model.StandardResponse;
import com.sun.net.httpserver.HttpExchange;

public final class HttpUtils {
    private HttpUtils() {
    }

    public static String getRequestBody(InputStream requestBody) {
        return new BufferedReader(new InputStreamReader(requestBody, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"));
    }

    public static Map<String, String> getFormDataParams(String formData) {
        return Arrays.stream(formData.split("&"))
                .map(param -> splitQueryParam(param))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static String getResponse(HttpExchange exchange, int responseCode, StandardResponse apiResponse)
            throws IOException {
        String response = new Gson().toJson(apiResponse);
        exchange.sendResponseHeaders(responseCode, response.length());
        return response;
    }

    public static String getResponse(HttpExchange exchange, int responseCode, String apiStatus, String message,
            Object data)
            throws IOException {
        String response = new Gson().toJson(new StandardResponse(apiStatus, message, new Gson().toJsonTree(data)));
        exchange.sendResponseHeaders(responseCode, response.length());
        return response;
    }

    public static String getResponse(HttpExchange exchange, int responseCode, String apiStatus, String message)
            throws IOException {
        return getResponse(exchange, responseCode, apiStatus, message, null);
    }

    private static Optional<Map.Entry<String, String>> splitQueryParam(String data) {

        try {
            data = decode(data);
            Pattern pattern = Pattern.compile("([^&=]+)=([^&]*)");
            Matcher matcher = pattern.matcher(data);
            /*
             * final int index = data.indexOf("=");
             * final String key = index > 0 ? data.substring(0, index) : data;
             * final String value = index > 0 && data.length() > index + 1 ?
             * data.substring(index + 1) : null;
             */

            if (matcher.matches()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                return Optional.of(Map.entry(key, value));
            } else {
                // Handle invalid format
                System.err.println("Invalid parameter format: " + data);
                return Optional.empty();
            }

        } catch (Exception e) {
            System.err.println("Invalid parameter format: " + data);
            return Optional.empty();
        }

        /* return new AbstractMap.SimpleImmutableEntry<>(key, value); */
    }

    public static boolean containsKeys(Map<String, ?> map, String... keys) {
        return Stream.of(keys).allMatch(map::containsKey);
    }

    public static String decode(String text) throws Exception {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw e;
        }
    }

    public static String validateReqMethod(HttpExchange exchange, String requestMethod)
            throws IOException {
        if (!requestMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            getResponse(exchange, HttpURLConnection.HTTP_BAD_METHOD, HttpStatus.ERROR.name(),
                    "Invalid Method");
        }
        return "";
    }

}

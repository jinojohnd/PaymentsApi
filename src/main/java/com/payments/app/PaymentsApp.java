package com.payments.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.payments.app.handlers.AmountTransferHandler;
import com.payments.app.handlers.ExternalWithdrawalHandler;
import com.payments.app.handlers.ExternalWithdrawalStatusHandler;
import com.payments.app.handlers.StatementHandler;
import com.payments.app.model.Account;
import com.payments.app.model.User;
import com.sun.net.httpserver.HttpServer;

public class PaymentsApp {
    private static Map<String, List<Account>> userAccountsMap = new ConcurrentHashMap<>();
    private static final Map<String, String> accountIdUserMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Starting server");
        try {
            // Init static Data
            try (InputStream inputStream = PaymentsApp.class.getClassLoader().getResourceAsStream("users.json")) {
                if (inputStream != null) {
                    try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                        List<User> users = new Gson().fromJson(reader, new TypeToken<List<User>>() {
                        }.getType());

                        users.parallelStream()
                                .forEach(user -> {
                                    userAccountsMap.put(user.id(), user.accounts());
                                    user.accounts()
                                            .forEach(account -> accountIdUserMap.put(account.accountId(), user.id()));
                                });
                    }
                } else {
                    throw new IOException("File not found!");
                }
            }

            System.out.println("Loaded static data");

            System.out.println("1. Get Balances API\r\n" + //
                    "Endpoint: GET /api/get-statement/{userId}\r\n" + //
                    "Description: Retrieves the balances for the specified user.\r\n" + //
                    "Parameters:\r\n" + //
                    "{userId}: User ID for which balances are requested.\r\n\r\n\r\n" + //
                    "2. Transfer Funds API\r\n" + //
                    "Endpoint: POST /api/transfer/{userId}\r\n" + //
                    "Description: Transfers funds from the user's account to another user's account.\r\n" + //
                    "Parameters:\r\n" + //
                    "{userId}: User ID initiating the transfer.\r\n" + //
                    "Request Body (urlencodedFormData):\r\n" + //
                    "ownAccountId: User's account from which the money is debited.\r\n" + //
                    "beneAccountId: The beneficiary user's account to which money is credited.\r\n" + //
                    "amount: Amount to be credited.\r\n\r\n\r\n" + //
                    "3. Transfer to External Account API\r\n" + //
                    "Endpoint: POST /api/external-acc-transfer/{userId}\r\n" + //
                    "Description: Transfers funds from the user's account to an external account.\r\n" + //
                    "Parameters:\r\n" + //
                    "{userId}: User ID initiating the transfer.\r\n" + //
                    "Request Body (urlencodedFormData):\r\n" + //
                    "ownAccountId: User's account from which the money is debited.\r\n" + //
                    "accAddress: The beneficiary account address.\r\n" + //
                    "amount: Amount to be credited.\r\n" + //
                    "Response: Returns the withdrawal ID for tracking.\r\n\r\n\r\n" + //
                    "4. Get External Transfer Status API\r\n" + //
                    "Endpoint: GET /api/external-acc-transfer-status\r\n" + //
                    "Description: Retrieves the status of an external withdrawal transfer.\r\n" + //
                    "Query Parameters:\r\n" + //
                    "withdrawalId: ID to track the status of the withdrawal.");
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

            server.createContext("/api/get-statement", new StatementHandler());
            server.createContext("/api/transfer", new AmountTransferHandler());
            server.createContext("/api/external-acc-transfer", new ExternalWithdrawalHandler());
            server.createContext("/api/external-acc-transfer-status", new ExternalWithdrawalStatusHandler());
            server.setExecutor(executor);
            server.start();

            System.out.println("Server started on port: 8080");
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    public static Map<String, List<Account>> getUserAccountsMap() {
        return userAccountsMap;
    }

    public static String getUserId(String accountId) {
        return accountIdUserMap.get(accountId);
    }
}

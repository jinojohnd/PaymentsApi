package com.payments.app.service;

import java.util.Map;

public interface AccountOperationsService {
    Map<Integer, String> deposit(String ownAccountId, String beneAccountId, String currentUser, Double amount)
            throws Exception;

    Map<Integer, String> initiateExternalAccountWithdrawal(String ownAccountId, String externalAccountAddr,
            String currentUser,
            Double amount) throws Exception;

    String getRequestState(String wId) throws Exception;
}

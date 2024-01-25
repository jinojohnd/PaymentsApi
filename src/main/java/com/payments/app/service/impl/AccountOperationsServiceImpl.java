package com.payments.app.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.payments.app.PaymentsApp;
import com.payments.app.model.Account;
import com.payments.app.service.AccountOperationsService;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.payments.app.service.impl.WithdrawalService.WithdrawalId;
import com.payments.app.service.impl.WithdrawalService.WithdrawalState;
import com.payments.app.service.impl.WithdrawalService.Address;

public class AccountOperationsServiceImpl implements AccountOperationsService {

    public static final WithdrawalService ws = new WithdrawalServiceStub();
    @Override
    public Map<Integer, String> deposit(String ownAccountId, String beneAccountId, String currentUserId, Double amount)
            throws Exception {
        try {
            Map<Integer, String> resultMap = new HashMap<>();
            Map<String, List<Account>> userAccountsMap = PaymentsApp.getUserAccountsMap();
            List<Account> userAccounts = userAccountsMap.get(currentUserId);

            String userId = PaymentsApp.getUserId(beneAccountId);
            if (null == userId) {
                resultMap.put(0, "Beneficiary Account not found");
                return resultMap;
            }

            List<Account> beneAccounts = userAccountsMap.get(userId);

            Account ownAccount = findAccountById(userAccounts, ownAccountId);
            Account beneAccount = findAccountById(beneAccounts, beneAccountId);

            synchronized (userAccountsMap) {
                if (ownAccount.balance() < amount) {
                    resultMap.put(0, "Insufficient Funds. Transaction Failed");
                    return resultMap;
                }
                userAccounts.remove(ownAccount);
                beneAccounts.remove(beneAccount);

                Account updatedOwnAccount = ownAccount.withBalance(ownAccount.balance() - amount);
                Account updatedBeneAccount = beneAccount.withBalance(beneAccount.balance() + amount);

                userAccounts.add(updatedOwnAccount);
                beneAccounts.add(updatedBeneAccount);
                resultMap.put(1, "Successfully processed the transaction");
                return resultMap;
            }
        } catch (Exception e) {
            System.err.println("Error -> " + e);
            throw e;
        }
    }

    @Override
    public Map<Integer, String> initiateExternalAccountWithdrawal(String ownAccountId, String externalAccountAddr,
            String currentUserId,
            Double amount) throws Exception {
        Map<Integer, String> resultMap = new HashMap<>();
        Map<String, List<Account>> userAccountsMap = PaymentsApp.getUserAccountsMap();
        List<Account> userAccounts = userAccountsMap.get(currentUserId);
        Account ownAccount = findAccountById(userAccounts, ownAccountId);

        UUID withdrawalId = UUID.randomUUID();
        synchronized (userAccountsMap) {
            if (ownAccount.balance() < amount) {
                resultMap.put(0, "Insufficient Funds. Transaction Failed");
                return resultMap;
            }

            ws.requestWithdrawal(new WithdrawalId(withdrawalId), new Address(externalAccountAddr), amount);
            userAccounts.remove(ownAccount);
            Account updatedOwnAccount = ownAccount.withBalance(ownAccount.balance() - amount);
            userAccounts.add(updatedOwnAccount);

            //Check after 10s and revert the amount if failed
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.schedule(() -> {
                WithdrawalState state = ws.getRequestState(new WithdrawalId(withdrawalId));
                if (state.equals(WithdrawalState.FAILED)) {
                    userAccounts.remove(updatedOwnAccount);
                    userAccounts.add(ownAccount.withBalance(updatedOwnAccount.balance() + amount));
                }

            }, 10, TimeUnit.SECONDS);

            // Shutdown the executor service after use
            executorService.shutdown();

        }
        resultMap.put(1,
                "Successfully processed the transaction. Withdrawal id for tracking: " + withdrawalId.toString());
        return resultMap;
    }

    @Override
    public String getRequestState(String wId) throws Exception {
        

        return ws.getRequestState(new WithdrawalId(UUID.fromString(wId))).name();
    }

    private Account findAccountById(List<Account> accounts, String accountId) {
        return accounts.stream()
                .filter(acc -> acc.accountId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    }

}

// Non Modifiable Stub

interface WithdrawalService {
    /**
     * Request a withdrawal for given address and amount. Completes at random moment
     * between 1 and 10 seconds
     * 
     * @param id      - a caller generated withdrawal id, used for idempotency
     * @param address - an address withdraw to, can be any arbitrary string
     * @param amount  - an amount to withdraw (please replace T with type you want
     *                to use)
     * @throws IllegalArgumentException in case there's different address or amount
     *                                  for given id
     */
    void requestWithdrawal(WithdrawalId id, Address address, Double amount); // Please substitute T with prefered type

    /**
     * Return current state of withdrawal
     * 
     * @param id - a withdrawal id
     * @return current state of withdrawal
     * @throws IllegalArgumentException in case there no withdrawal for the given id
     */
    WithdrawalState getRequestState(WithdrawalId id);

    enum WithdrawalState {
        PROCESSING, COMPLETED, FAILED
    }

    record WithdrawalId(UUID value) {
    }

    record Address(String value) {
    }
}

class WithdrawalServiceStub implements WithdrawalService {
    private final ConcurrentMap<WithdrawalId, Withdrawal> requests = new ConcurrentHashMap<>();

    @Override
    public void requestWithdrawal(WithdrawalId id, Address address, Double amount) { // Please substitute T with
                                                                                     // prefered type
        final var existing = requests.putIfAbsent(id, new Withdrawal(finalState(), finaliseAt(), address, amount));
        if (existing != null && !Objects.equals(existing.address, address) && !Objects.equals(existing.amount, amount))
            throw new IllegalStateException("Withdrawal request with id[%s] is already present".formatted(id));
    }

    private WithdrawalState finalState() {
        return ThreadLocalRandom.current().nextBoolean() ? WithdrawalState.COMPLETED : WithdrawalState.FAILED;
    }

    private long finaliseAt() {
        return System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(1000, 10000);
    }

    @Override
    public WithdrawalState getRequestState(WithdrawalId id) {
        final var request = requests.get(id);
        if (request == null)
            throw new IllegalArgumentException("Request %s is not found".formatted(id));
        return request.finalState();
    }

    record Withdrawal(WithdrawalState state, long finaliseAt, Address address, Double amount) {
        public WithdrawalState finalState() {
            return finaliseAt <= System.currentTimeMillis() ? state : WithdrawalState.PROCESSING;
        }
    }
}

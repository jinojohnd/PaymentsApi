# PaymentsApi

## Get Balances API

**Endpoint:** `GET /api/get-statement/{userId}`

**Description:** Retrieves the balances for the specified user.

**Parameters:**
- `{userId}`: User ID for which balances are requested.

---

## Transfer Funds API

**Endpoint:** `POST /api/transfer/{userId}`

**Description:** Transfers funds from the user's account to another user's account.

**Parameters:**
- `{userId}`: User ID initiating the transfer.

**Request Body (urlencodedFormData):**
- `ownAccountId`: User's account from which the money is debited.
- `beneAccountId`: The beneficiary user's account to which money is credited.
- `amount`: Amount to be credited.

---

## Transfer to External Account API

**Endpoint:** `POST /api/external-acc-transfer/{userId}`

**Description:** Transfers funds from the user's account to an external account.

**Parameters:**
- `{userId}`: User ID initiating the transfer.

**Request Body (urlencodedFormData):**
- `ownAccountId`: User's account from which the money is debited.
- `accAddress`: The beneficiary account address.
- `amount`: Amount to be credited.

**Response:** Returns the withdrawal ID for tracking.

---

## Get External Transfer Status API

**Endpoint:** `GET /api/external-acc-transfer-status`

**Description:** Retrieves the status of an external withdrawal transfer.

**Query Parameters:**
- `withdrawalId`: ID to track the status of the withdrawal.

---

**Note:**
- Ensure that you replace `{userId}` in the URLs with the actual user ID during API calls.
- For starting the server, go to the release folder and run `java -jar payments-api.jar`.

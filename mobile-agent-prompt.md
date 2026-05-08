# Mobile Agent Prompt: Authentication API Integration

## Overview
The Spring Boot backend exposes a JWT-based authentication API at `/api/v1/auth`. All endpoints are stateless — the mobile app owns the session by storing and sending the JWT. Two recent work streams are reflected here: a security hardening pass and a new Google social login endpoint.

---

## Required Headers on Every Request
Every request to this API (except `/actuator/**`) must include these three headers or the server returns `400 Bad Request`:

```
ipaddress: <client IP address>
location: <user location string>
appInstallationId: <unique device installation identifier>
```

---

## Common Response Schemas

**JWT Authentication Response** (returned on successful login/refresh):
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "appInstallationId": "string",
  "biometricToken": "string | null"
}
```

**Validation Error Response** (`400 Bad Request` — missing or invalid fields):
```json
{
  "status": "FAILED",
  "message": "Validation failed",
  "data": {
    "fieldName": "error message"
  }
}
```

**Rate Limit Response** (`429 Too Many Requests` — 5+ failed logins in 15 min):
No body. Back off and prompt the user to wait before retrying.

---

## Endpoints

### 1. Login — `POST /api/v1/auth/login`

**Request:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["username", "password"],
  "properties": {
    "username": { "type": "string", "minLength": 1 },
    "password": { "type": "string", "minLength": 1 }
  }
}
```

**Responses:**
- `200` → JWT Authentication Response (includes `biometricToken` — store it securely for biometric login)
- `400` → Validation error
- `401` → Bad credentials
- `429` → Too many failed attempts — locked for 15 minutes

---

### 2. Refresh Token — `POST /api/v1/auth/refresh`

**Request header:**
```
Authorization: Bearer <refreshToken>
```
No body.

**Important:** Only the `refreshToken` is accepted here. Sending an access token returns `401`.

**Responses:**
- `200` → JWT Authentication Response (new access token; same refresh token returned)
- `401` → Invalid, expired, or wrong token type

---

### 3. Change Password — `POST /api/v1/auth/change-password`

Requires `Authorization: Bearer <accessToken>` header.

**Request:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["oldPassword", "newPassword"],
  "properties": {
    "oldPassword": { "type": "string", "minLength": 1 },
    "newPassword": { "type": "string", "minLength": 8 }
  }
}
```

**Important:** After a successful password change, all existing access tokens for this user are immediately invalidated. The user must log in again to get a new token.

**Responses:**
- `200` → `{"message": "Password changed successfully"}`
- `400` → Invalid old password or validation error
- `401` → Not authenticated

---

### 4. Request Password Reset — `POST /api/v1/auth/reset-password`

**Request:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["username"],
  "properties": {
    "username": { "type": "string", "minLength": 1 }
  }
}
```

**Responses:**
- `200` → `{"message": "If the username exists, a reset code has been sent."}` (always 200 regardless of whether user exists)
- `400` → Validation error

---

### 5. Confirm Password Reset — `POST /api/v1/auth/reset-password/confirm`

**Request:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["username", "token", "newPassword"],
  "properties": {
    "username": { "type": "string", "minLength": 1 },
    "token": { "type": "string", "minLength": 1, "description": "6-digit OTP sent to user" },
    "newPassword": { "type": "string", "minLength": 8 }
  }
}
```

OTP expires after **15 minutes**.

**Responses:**
- `200` → `{"message": "Password has been reset successfully. You can now login."}`
- `400` → Invalid/expired token or validation error

---

### 6. Biometric Login — `POST /api/v1/auth/login/biometric`

**Request:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["username", "biometricToken"],
  "properties": {
    "username": { "type": "string", "minLength": 1 },
    "biometricToken": {
      "type": "string",
      "minLength": 1,
      "description": "Raw biometric token returned during last login"
    }
  }
}
```

**Important:** The `biometricToken` rotates on every successful biometric login. Always store the latest one returned in the response. The previous token is immediately invalidated.

**Responses:**
- `200` → JWT Authentication Response (includes new `biometricToken` — update stored value)
- `401` → Invalid biometric token

---

### 7. Google Login — `POST /api/v1/auth/login/google`

The mobile app obtains a Google ID token via the Google Sign-In SDK, then sends it here. The backend verifies it with Google and issues a JWT. The user is auto-registered on first login.

**Request:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["idToken"],
  "properties": {
    "idToken": {
      "type": "string",
      "minLength": 1,
      "description": "Google ID token obtained from Google Sign-In SDK"
    }
  }
}
```

**Responses:**
- `200` → JWT Authentication Response (no `biometricToken`)
- `400` → Validation error (missing token)
- `401` → Google token invalid, expired, or issued for wrong client

---

## Token Storage & Lifecycle

| Token | Where to store | Lifetime | Notes |
|---|---|---|---|
| `accessToken` | Secure in-memory | 24 hours | Send as `Authorization: Bearer <token>` |
| `refreshToken` | Secure persistent storage (Keychain/Keystore) | 7 days | Use only at `/refresh` |
| `biometricToken` | Secure persistent storage (Keychain/Keystore) | Until next biometric login | Rotates on every use — always update stored value |

**Token invalidation:** Access tokens are invalidated immediately after `/change-password`. Catch `401` on any authenticated call and redirect to login.

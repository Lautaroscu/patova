# NumGuard — Data Safety & Privacy Declaration

## Play Console Data Safety Questionnaire Answers

This document provides the answers required for the Google Play Console **Data safety** section for the NumGuard application (`ar.com.numguard`).

---

## 1. Data Collection & Purpose

### Does your app collect or share any of the required user data types?

**Yes.** NumGuard collects the following data types exclusively for app functionality and security purposes.

| Data Type | Collected? | Purpose | Required for App? |
|---|---|---|---|
| **Phone Number (hashed)** | Yes | App functionality, Fraud prevention, Security | Yes |
| **Call Log** | Yes (processed locally) | App functionality, Fraud prevention | Yes |
| **Contacts** | Yes (processed locally) | App functionality, Spam filtering | Yes |
| **Device ID** | Yes | App functionality, Fraud prevention | Yes |
| **Crash Logs** | No | N/A | No |
| **Location** | No | N/A | No |

### Detailed Explanation of Each Data Type

#### Phone Number
- **What:** The incoming caller's phone number.
- **How it's used:** The number is intercepted via `CallScreeningService` only during an incoming call. Before any network transmission, the number is transformed into a **one-way SHA-256 cryptographic hash**. The raw phone number never leaves the device and is never stored in plaintext on any server.
- **Why it's needed:** To check whether the incoming call is associated with known fraud patterns (e.g., ENACOM-reported scam numbers).

#### Call Log
- **What:** Records of incoming calls.
- **How it's used:** Processed entirely on-device using Room database (local storage). Used to show the user their call history within the app and to detect repeat spam callers.
- **Why it's needed:** The app functions as a call screening and spam-blocking tool. Historical view is a core feature.

#### Contacts
- **What:** The user's locally stored contacts.
- **How it's used:** Read entirely on-device to create an automatic whitelist. Calls from known contacts are never blocked or flagged. Contact data never leaves the device.
- **Why it's needed:** To prevent false positives — blocking calls from known contacts would degrade the user experience.

#### Device ID
- **What:** A unique, anonymous device identifier (Android ID or equivalent).
- **How it's used:** Used server-side for rate-limiting and abuse prevention. Not linked to any personal identity.
- **Why it's needed:** To prevent API abuse and ensure fair usage of the cloud validation service.

---

## 2. Data Sharing

**NumGuard does NOT share user data with any third party.**

- No data is sold, rented, or traded.
- No data is shared with advertisers, analytics providers, or data brokers.
- No data is transferred to any third-party service.

The only data transmitted off-device is:
1. **SHA-256 hashes** of incoming phone numbers — sent to NumGuard's own API server for fraud lookup.
2. **Anonymous device ID** — for rate-limiting purposes.

Both are sent exclusively to `api.numguard.com.ar` over HTTPS/TLS.

---

## 3. Data Encryption

### Encryption in Transit

All network communication between the NumGuard Android app and the NumGuard backend API is encrypted using **TLS 1.2+ (HTTPS)**. The app's `network_security_config.xml` enforces secure connections.

### Encryption at Rest

- **On-device:** Call history and cached validations are stored in an SQLite database via Room, which uses Android's built-in file-level encryption when the device is encrypted (all modern Android devices).
- **On-server:** Hashed phone numbers and verdicts are stored in an encrypted database. The SHA-256 hashing ensures that even if server data were compromised, the original phone numbers cannot be recovered (one-way cryptographic function).

---

## 4. Data Retention & Deletion

| Data | Storage Location | Retention Period | Deletion Mechanism |
|---|---|---|---|
| Call Log | Device only (Room DB) | 30 days rolling | Automatic purge |
| Contact List | Never stored | Read in-memory only | N/A |
| Phone Hashes | Server | 90 days for cached verdicts | Automatic TTL expiration |
| Device ID | Server | Account lifetime | Deleted on app uninstall request |

### User Data Deletion Request

Users can request deletion of their server-side data at any time by:
1. Navigating to **Profile > Delete My Data** within the app.
2. Emailing `privacidad@numguard.com.ar` with the subject "Solicitud de eliminacion de datos".

Upon confirmation, all hashed phone numbers and device identifiers associated with the requesting installation are permanently deleted within **30 days**.

---

## 5. Permissions Declared

The following Android permissions are declared in the manifest and their use is justified below:

| Permission | Purpose | Justification |
|---|---|---|
| `READ_PHONE_STATE` | Detect incoming calls | Required by `CallScreeningService` to intercept and screen calls |
| `READ_CALL_LOG` | Display call history | Core app feature — users can view their screened calls |
| `READ_CONTACTS` | Whitelist known contacts | Prevents blocking calls from the user's saved contacts |
| `ANSWER_PHONE_CALLS` | Call management | Required for call blocking/silencing functionality |
| `POST_NOTIFICATIONS` | Blocked-call alerts | Notify user when a call has been blocked |
| `INTERNET` | Cloud validation | Required to query the NumGuard fraud database |
| `BIND_SCREENING_SERVICE` | Call screening | Required for `CallScreeningService` system binding |

---

## 6. Prominent Disclosure Notice

**As required by Google Play's "Prominent Disclosure" policy for sensitive permissions:**

The app presents a full-screen explanation **before** any native Android permission dialog appears. This disclosure screen:

1. Explains that call permissions are required to intercept fraudulent numbers.
2. States that phone numbers are transformed into **irreversible SHA-256 cryptographic hashes** before any server verification.
3. Confirms that **no raw phone numbers or personally identifiable data is stored or sold**.
4. Requires the user to explicitly tap **"Aceptar y Continuar"** before any permission is requested.

Users who decline are gracefully directed to a screen explaining that the app cannot function without these permissions, with an option to open system Settings.

---

## 7. Compliance Checklist

- [x] Prominent disclosure shown before permission requests
- [x] In-transit encryption (TLS 1.2+) for all network traffic
- [x] One-way hashing (SHA-256) for phone numbers before transmission
- [x] No raw phone numbers stored on server
- [x] No data sharing with third parties
- [x] No advertising or analytics SDKs
- [x] User data deletion mechanism documented
- [x] All declared permissions have explicit justifications
- [x] No unused or unnecessary permissions declared
- [x] Foreground service types declared for Android 14+ compliance

---

Prepared for Google Play Console submission.
Last updated: May 2025

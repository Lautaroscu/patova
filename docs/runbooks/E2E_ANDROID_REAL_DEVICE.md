# E2E Manual Testing Runbook - NumGuard Android App (Real Device)

## Prerequisites

- Android device with USB debugging enabled or APK sideload capability.
- Backend API running and accessible from the device (local network or staging env).
- API key configured (`NUMGUARD_API_KEY`).
- Seed data loaded (spam numbers, prefixes).

## Setup

1. **Build debug APK**
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
   APK located at: `android/app/build/outputs/apk/debug/app-debug.apk`

2. **Install APK on device**
   ```bash
   adb install android/app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Configure API base URL**
   - Open NumGuard on device.
   - Navigate to Settings.
   - Set `API Base URL` to your backend (e.g. `http://192.168.1.100:8000`).
   - Set the API key in `X-NumGuard-Key` field.

## E2E Checklist

### 1. Install APK debug
- [ ] `adb install` succeeds without errors.
- [ ] App icon appears on launcher.

### 2. Configure API
- [ ] Backend is running and reachable from device.
- [ ] Health check returns `{"status":"ok"}` from device browser.

### 3. Activate NumGuard as Call Screening App
- [ ] Go to Android Settings > Apps > Default Apps > Caller ID & Spam.
- [ ] Set NumGuard as the default caller ID/spam app.

### 4. Seed Known Spam Number
- [ ] Run seed script or insert via API a known spam number (e.g. `+541199999999`).
- [ ] Verify via `GET /v1/number/+541199999999` that status is `SPAM`.

### 5. Make Call from Spam Test Number
- [ ] Use a second phone or VoIP service to call the test device from the spam number.
- [ ] Observe NumGuard intercepts the call.

### 6. Verify Block/Reject
- [ ] Incoming call blocked/rejected automatically if verdict is BLOCK.
- [ ] Call log shows the blocked call.

### 7. Verify Notification
- [ ] Android notification appears: "NumGuard blocked a spam call from +541199999999".
- [ ] Notification is actionable (dismiss/view details).

### 8. Verify Call History
- [ ] Open NumGuard app.
- [ ] Navigate to History tab.
- [ ] Blocked call appears with timestamp, number, and verdict.

### 9. Mark False Positive
- [ ] Tap on a blocked call in history.
- [ ] Select "Mark as False Positive".
- [ ] Confirm the action.

### 10. Verify Backend Receives Feedback
- [ ] Check backend logs or admin panel.
- [ ] Confirm `POST /v1/feedback` was received with `feedback_type: FALSE_POSITIVE`.
- [ ] Verify spam score for that number decreased.

### 11. Offline Mode - Fail Open
- [ ] Enable Airplane mode on the device.
- [ ] Make a test call from an unknown number.
- [ ] NumGuard should allow the call through (fail-open behavior).
- [ ] No crash or ANR dialog.

### 12. Confirm Fail-Open
- [ ] Disable Airplane mode.
- [ ] NumGuard reconnects and resumes normal operation.
- [ ] Pending reports (if any) are synced.

## Expected Results

| Step | Expected Behavior |
|------|-------------------|
| 3 | NumGuard appears as call screening option |
| 6 | Spam call blocked automatically |
| 7 | Notification shown with spam details |
| 8 | History entry created |
| 10 | Backend feedback endpoint receives POST |
| 11 | Call allowed through (fail-open) |

## Troubleshooting

- **API unreachable**: Verify device and backend are on same WiFi network. Check firewall.
- **App crashes**: Run `adb logcat | grep -i numguard` to capture logs.
- **Call not intercepted**: Verify default caller ID app is set to NumGuard.
- **No notification**: Check Android notification permissions for NumGuard.

## Sign-off

| Tester | Date | Result | Notes |
|--------|------|--------|-------|
|        |      |        |       |

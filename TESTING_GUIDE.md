# Testing Guide - WAY App Phase 3 Fixes

## Pre-Testing Setup

### Required
1. Android device or emulator (API 28+)
2. Geoapify API key: `3666750b67f448579f819336f91be739` (already in `local.properties`)
3. Firebase configured (already set up)
4. Phone with SMS/calling capability or emulator with those features enabled

### Build & Install
```bash
cd C:\Users\Dell\AndroidStudioProjects\WAY
.\gradlew.bat :app:assembleDebug
# Then install on device:
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Test Case 1: Existing Account Login (Login Loop Fix)

**Objective:** Verify existing users don't get redirected to setup wizard

**Steps:**
1. Sign up with email and complete entire setup wizard
2. Go to Settings > Sign Out
3. Sign in again with same credentials
4. **Expected:** Should go directly to Dashboard, NOT setup wizard
5. **Alternative:** Test with Google Sign-In for existing account

**Verification Points:**
- Login → Dashboard (not Setup)
- Dashboard shows user name and analytics
- "Start Walk" button is visible and clickable

**What was broken:**
- After login, `authViewModel.isSetupComplete()` was always false for Google users
- Now: Properly fetches `setupComplete` from Firestore

---

## Test Case 2: New User Signup (No Regression)

**Objective:** Ensure new users still go through setup wizard

**Steps:**
1. Uninstall app or clear app data
2. Launch app
3. Click "Sign Up"
4. Create new account with email/password (or Google)
5. **Expected:** Should go to Setup Wizard, not Dashboard

**Verification Points:**
- Step 1: Add contacts (name + phone) - can add 1+ contacts
- Step 2: Add locations (optional) - can add locations with GPS capture
- Step 3: Set emergency code (4+ digit PIN)
- Step 4: Grant permissions - can proceed after permissions
- Finish → Dashboard

---

## Test Case 3: Setup Locations (Setup Wizard Locations Step)

**Objective:** Verify location adding works during setup

**Steps:**
1. Start a new account or replay setup (clear `PREF_SETUP_COMPLETE` in SharedPrefs)
2. Get to Setup Step 2: "Add Locations"
3. Tap the **+** button
4. **Expected:** Permission dialog appears
5. Grant location permission
6. **Expected:** Dialog appears with GPS coordinates (from device or emulator location)
7. Enter location name (e.g., "Home")
8. Optionally enter address (e.g., "123 Main St")
9. Tap "Add"
10. **Expected:** Location appears in list, can be deleted with long-press menu
11. Tap "Next" to proceed to Step 3

**Verification Points:**
- GPS is captured (show coordinates in debug logs)
- Location saves to Firestore and shows in list
- Can add multiple locations
- Can delete locations before proceeding
- Step is optional (can skip and proceed without locations)

**What was broken:**
- SetupLocationsFragment was just a placeholder
- GPS wasn't being captured
- No location list or FAB visible
- Now: Fully functional with GPS capture and repository integration

---

## Test Case 4: Add Location from Settings (Manage Locations)

**Objective:** Verify location adding works in settings

**Steps:**
1. Go to Dashboard → Settings (gear icon)
2. Find "Manage Locations" card/button
3. Tap it → Frequent Locations screen appears
4. Tap **+** button (FAB)
5. **Expected:** Permission request
6. Grant location permission
7. **Expected:** Dialog appears with GPS already populated
8. Enter name: "Work"
9. Enter address: "456 Oak Ave"
10. Tap "Add"
11. **Expected:** Location appears in list
12. **Verify:** Location also appears in Walk Destination screen suggestions

**Verification Points:**
- GPS is automatically captured
- Location saved to Firestore with lat/lng
- Shows in saved locations list
- Shows in destination selection on start walk
- Can edit (tap location → edit menu) and re-save
- Can delete

**What was broken:**
- GPS capture was async but dialog showed before GPS was ready
- Coordinates defaulted to 0.0, 0.0 if geocoding failed
- Now: GPS captured first, used as fallback for geocoding

---

## Test Case 5: Walk Session History (Duplicate Saves Fix)

**Objective:** Verify walks appear only once in history

**Steps:**
1. Start a walk (any destination)
2. Walk for ~30 seconds to 1 minute
3. Tap "End Walk"
4. Go to History screen
5. **Expected:** One entry for this walk
6. Check the entry shows correct:
   - Destination name
   - Duration
   - Distance
   - Alert status
7. Check Firestore console:
   - Navigate to `users/{uid}/walks/{sessionId}`
   - Should see ONE document (not duplicated)

**Verification Points:**
- Only 1 entry in list per walk
- Entry has correct metadata
- Firestore has no duplicate docs
- Total walk count in dashboard is accurate

**What was broken:**
- `saveSession()` always created new doc even if session had ID
- Now: Reuses doc ID if present, only creates new if blank

---

## Test Case 6: Inactivity Detection (Movement Confirmation)

**Objective:** Verify inactivity only triggers after confirmed inactivity

**Steps:**
1. Start a walk to any destination
2. Stand still for 60+ seconds (1 minute)
3. **Expected:** Safety Check dialog appears (inactivity trigger)
4. Dismiss the check
5. **Expected:** Inactivity trigger and screen return to walking

**Verification:**
- Walk around normally with frequent movements
- **Ensure:** Safety check does NOT trigger just from normal movement
- Single noisy GPS points should NOT reset inactivity
- Only after 2 consecutive confirmed readings should reset

**Debug Verification:**
```
adb logcat WalkService:I
```

Look for logs like:
- `Movement confirmed` (when movement detected)
- `Inactivity triggered` (when safety check triggers)

**What was broken:**
- Single noisy GPS point could reset the 60s timer
- Now: Requires 2 consecutive valid movement readings

**Current Threshold Details:**
- Inactivity: 60 seconds
- Min movement: 6 meters
- Movement confirmation: 2 readings
- Min speed for movement: 1.2 m/s
- GPS accuracy filter: 45 meters (ignore noisy points)

---

## Test Case 7: Emergency Call with Locked Phone

**Objective:** Verify inactivity can trigger call even with phone locked

**Prerequisites:**
- Have emergency contacts added
- Contact priority set correctly
- Phone has cellular/calling enabled

**Steps:**
1. Start a walk
2. Lock the phone
3. Stand still for 60+ seconds
4. **Expected:**
   - Safety Check dialog appears on lock screen
   - OR call is automatically placed to priority contact
   - Phone wakes up
   - Call UI visible over lock screen

**Verification:**
- Phone screen turns on even when locked
- Dialer/call app launches
- Call connects to emergency contact
- SMS also sent (check contact's phone or notification)

**Debug Logs:**
```
adb logcat EmergencyHandler:I
```

Look for:
- `Acquired wake lock`
- `Calling {contact_name}`
- `SMS sent to {contact_name}`

**What was fixed:**
- Added `PowerManager.PARTIAL_WAKE_LOCK` acquisition
- Intent flags: `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`
- Wake lock released after 30 seconds
- AndroidManifest now includes `<uses-permission android:name="android.permission.WAKE_LOCK" />`

---

## Test Case 8: Walk Destination with Saved Locations

**Objective:** Verify saved locations appear in destination selection

**Steps:**
1. Add at least 2 locations from Settings
2. Go to Start Walk
3. Tap in destination field or view "Saved Locations" section
4. **Expected:** See all saved locations listed
5. Tap one of them
6. **Expected:**
   - Location name appears in destination field
   - GPS coordinates set correctly
   - Start walk proceeds

**Verification Points:**
- All saved locations visible
- GPS coordinates populated from saves
- Walk session shows correct destination

---

## Test Case 9: Location Geocoding Fallback

**Objective:** Verify location is saved even if geocoding fails

**Steps:**
1. From Settings, add a new location
2. Enter gibberish address that won't geocode (e.g., "xyzabc12345")
3. Tap "Add"
4. **Expected:** Location still saves with:
   - GPS captured at save time (fallback)
   - Name preserved
   - Gibberish address preserved
5. Verify in destination selection: location is selectable

**Verification Points:**
- No crash or error
- Location appears in list
- Location can be used as walk destination

**What was fixed:**
- Previously: Geocoding fail → coordinates = 0.0, 0.0
- Now: Uses GPS fallback if geocoding fails

---

## Quick Verification Checklist

After all tests pass:

- [ ] Existing user login → Dashboard (not setup)
- [ ] New user signup → Setup wizard complete  
- [ ] Setup step 2 → Can add locations with GPS
- [ ] Settings → Can add locations with GPS
- [ ] History → One entry per walk (no duplicates)
- [ ] Inactivity → Triggers at 60s, not sooner
- [ ] Locked phone → Call triggers and wakes device
- [ ] Destinations → Show saved locations with correct GPS
- [ ] Geocoding fail → Location still saves with GPS fallback
- [ ] App compiles → No warnings or errors

---

## Debug Commands

```bash
# View app logs
adb logcat com.example.way:V

# View Firestore saves (requires Firebase debugging)
adb logcat *:S com.google.firebase:V

# Check wake lock usage
adb shell dumpsys power | grep -i "wake"

# Check location updates
adb logcat WalkService:V | grep -i "location"

# Check auth state
adb logcat AuthViewModel:V
```

---

## Known Limitations

1. **Emulator Limitations:**
   - GPS simulation needs to be configured in emulator settings
   - Location permission must be explicitly granted after app launch
   - Call/SMS won't actually dial but intent will be logged

2. **Firebase Requirements:**
   - Internet connection required for location save
   - If offline, locations saved locally but need internet to sync
   - Firestore writes have 8 second timeout per operation

3. **Device Specifics:**
   - Wake lock only works on Android 5.0+
   - Call screen on lock screen works best on Android 8.0+
   - SMS may be delayed or blocked by device carrier



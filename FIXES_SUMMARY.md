# WAY App - Bug Fixes Summary (Phase 3)

## Issues Fixed

### 1. **Existing Accounts Triggering Setup Wizard on Login** ✅
**Problem:** Users who already completed setup were being redirected to setup wizard again after login.

**Root Cause:** In `AuthRepositoryImpl`, the `signInWithGoogle()` method was always setting `setupComplete = true` for existing users, even if they hadn't completed setup in Firestore.

**Fix:**
- Updated `signInWithEmail()` to fetch the `setupComplete` flag from Firestore
- Updated `signInWithGoogle()` to respect the actual `setupComplete` flag from Firestore instead of hardcoding it to `true`
- File: `app/src/main/java/com/example/way/data/repository/AuthRepositoryImpl.kt`

### 2. **Walk Sessions Counted/Shown Twice in History** ✅
**Problem:** Each walk appeared twice in history and was saved twice to Firestore.

**Root Cause:** `WalkSessionRepositoryImpl.saveSession()` was always creating a new document instead of checking if the session already had an ID.

**Fix:**
- Modified `saveSession()` to reuse existing document ID if provided, instead of always creating new
- Added timeout check to ensure successful save
- File: `app/src/main/java/com/example/way/data/repository/WalkSessionRepositoryImpl.kt`

### 3. **Emergency Calls Not Working When Phone Locked** ✅
**Problem:** When phone is locked and inactivity triggers, the call wasn't being placed.

**Root Cause:** Missing wake lock acquisition and insufficient activity flags to display call UI over lock screen.

**Fix:**
- Added `WAKE_LOCK` permission to AndroidManifest.xml
- Modified `EmergencyHandler.dialPriorityContact()` to:
  - Acquire `PowerManager.PARTIAL_WAKE_LOCK` before dialing
  - Use proper intent flags: `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`
  - Use `Handler.postDelayed()` to release wake lock after 30 seconds
  - Use extension function `.toUri()` instead of `Uri.parse()`
- File: `app/src/main/java/com/example/way/service/EmergencyHandler.kt`

### 4. **Cannot Add Locations from Settings** ✅
**Problem:** "Manage Locations" button didn't capture GPS and locations weren't being saved.

**Root Cause:** GPS capture was asynchronous but dialog was being shown before GPS data arrived. Also, GPS fallback wasn't working properly.

**Fix:**
- Added location permission launcher in `FrequentLocationsFragment`
- Modified workflow to:
  1. Request permission first
  2. Capture GPS in background
  3. Show dialog only after GPS is captured
  4. Pass GPS to ViewModel which uses it as fallback for geocoding
- Updated `FrequentLocationsViewModel` to accept optional GPS parameter
- If geocoding fails, uses provided GPS or preserves existing coordinates instead of defaulting to 0.0
- Files:
  - `app/src/main/java/com/example/way/ui/settings/FrequentLocationsFragment.kt`
  - `app/src/main/java/com/example/way/ui/settings/FrequentLocationsViewModel.kt`

### 5. **Setup Wizard Not Allowing Location Adding** ✅
**Problem:** Setup locations step was just a placeholder showing "Coming Soon".

**Root Cause:** `SetupLocationsFragment` had no implementation and layout only showed a message.

**Fix:**
- Completely rewrote `SetupLocationsFragment` to:
  - Show functional RecyclerView of added locations
  - Capture GPS when adding locations via permission launcher
  - Support add/delete of locations during setup
  - Remain optional (validation always returns true)
- Updated layout `fragment_setup_locations.xml` to include:
  - RecyclerView for location list
  - FloatingActionButton to add locations
  - Empty state hint
- Files:
  - `app/src/main/java/com/example/way/ui/onboarding/SetupLocationsFragment.kt`
  - `app/src/main/res/layout/fragment_setup_locations.xml`

### 6. **Inactivity Detection Too Sensitive** ✅
**Problem:** Single noisy GPS points were resetting the inactivity timer.

**Root Cause:** Movement confirmation logic was allowing any single reading to reset inactivity.

**Fix:**
- Added confirmed movement tracking:
  - `MIN_MOVEMENT_SPEED_MPS = 1.2f` (new threshold)
  - `MOVEMENT_CONFIRMATION_READINGS = 2` (require 2 consecutive valid readings)
  - `movementEvidenceReadings` counter tracks consecutive movement detections
  - Only resets `lastMovementTime` after 2 consecutive valid movements
- Modified logic in `WalkForegroundService.processLocation()` to:
  - Check both distance AND speed-with-distance
  - Increment counter only for valid movements
  - Reset inactivity timer only after confirmation threshold is met
- Files:
  - `app/src/main/java/com/example/way/util/Constants.kt`
  - `app/src/main/java/com/example/way/service/WalkForegroundService.kt`

### 7. **Locations Not Persisting GPS Coordinates** ✅
**Problem:** Saved locations were stored with coordinates but GPS wasn't being captured.

**Root Cause:** When adding locations, GPS wasn't being passed to the save operation.

**Fix:**
- Model already had latitude/longitude fields
- Implemented GPS capture in settings and setup fragments
- GPS is now captured and passed through the entire save flow
- File: `app/src/main/java/com/example/way/data/model/FrequentLocation.kt` (already has fields)

## Current Thresholds

### Inactivity Detection (Constants.kt):
- `INACTIVITY_THRESHOLD_SECONDS = 60L` (1 minute without movement)
- `MIN_MOVEMENT_METERS = 6f` (minimum distance for movement)
- `MIN_MOVEMENT_SPEED_MPS = 1.2f` (minimum speed for movement validation)
- `MOVEMENT_CONFIRMATION_READINGS = 2` (require 2 consecutive readings)
- `MIN_VALID_GPS_ACCURACY_METERS = 45f` (ignore noisy GPS points)
- `SAFETY_TRIGGER_COOLDOWN_MS = 45_000L` (prevent repeated triggers)

### Speed Detection:
- `SPEED_THRESHOLD_MPS = 4.2f` (~15 km/h)
- `SPEED_SUSTAINED_READINGS = 3` (3 consecutive readings trigger alert)

### Fall Detection:
- `FALL_MAGNITUDE_LOW = 2.0f` (free-fall threshold)
- `FALL_MAGNITUDE_HIGH = 20.0f` (impact threshold)
- `FALL_DETECTION_WINDOW_MS = 500L` (time window for detection)

## Files Modified

1. `app/src/main/java/com/example/way/util/Constants.kt` - Added movement confirmation thresholds
2. `app/src/main/java/com/example/way/service/WalkForegroundService.kt` - Improved inactivity logic
3. `app/src/main/java/com/example/way/data/repository/WalkSessionRepositoryImpl.kt` - Fixed duplicate saves
4. `app/src/main/java/com/example/way/data/repository/LocationRepositoryImpl.kt` - Added logging
5. `app/src/main/java/com/example/way/data/repository/AuthRepositoryImpl.kt` - Fixed auth flow
6. `app/src/main/java/com/example/way/service/EmergencyHandler.kt` - Added wake lock & call flags
7. `app/src/main/java/com/example/way/ui/settings/FrequentLocationsFragment.kt` - GPS capture workflow
8. `app/src/main/java/com/example/way/ui/settings/FrequentLocationsViewModel.kt` - GPS fallback in geocoding
9. `app/src/main/java/com/example/way/ui/onboarding/SetupLocationsFragment.kt` - Full rewrite with functionality
10. `app/src/main/res/layout/fragment_setup_locations.xml` - Updated layout
11. `app/src/main/AndroidManifest.xml` - Added WAKE_LOCK permission

## Testing Checklist

- [ ] Login with existing account - should go to dashboard, not setup
- [ ] New user signup - should go through setup wizard
- [ ] Setup wizard locations step - can add/delete locations with GPS
- [ ] Add location from settings - captures GPS and saves
- [ ] Start walk and stand still for 60+ seconds - inactivity triggers
- [ ] Walk normally and move around - inactivity doesn't trigger
- [ ] Phone locked + inactivity trigger - call is placed (check device logs)
- [ ] History - only one entry per walk session
- [ ] Locations saved - appear in destination selection screen

## Notes

- GeoapifyService is already configured with API key in `local.properties`
- All GPS operations wrapped with permission checks
- All async operations use proper coroutine scopes
- Firestore timeout: 8 seconds per operation
- All changes maintain backward compatibility


# Phase 4: Quick Start & Testing Checklist

## Pre-Test Verification ✅

- [ ] Geoapify API key in `local.properties`: `GEOAPIFY_API_KEY=3666750b67f448579f819336f91be739`
- [ ] Build succeeds without compilation errors
- [ ] APK installed on test device
- [ ] Test user created and logged in

---

## Feature Tests

### Test 1: Frequent Locations in Settings
**Location**: Settings → Frequent Locations

- [ ] FAB button is visible
- [ ] Tap FAB → Dialog opens with empty name/address fields
- [ ] Start typing address (e.g., "Eiffel")
- [ ] Geoapify suggestions appear below 3+ characters
- [ ] Suggestions show name and full address
- [ ] Click a suggestion → Fields auto-populate
- [ ] Location name shows full place name
- [ ] Location address shows complete address
- [ ] Tap Add → Location saved to list
- [ ] New location appears in list with icon and details
- [ ] Long-press location → Popup menu appears (Edit/Delete)
- [ ] Edit → Dialog with pre-filled name and address
- [ ] Edit location address → Autocomplete still works
- [ ] Delete → Confirmation dialog appears
- [ ] Confirm delete → Location removed from list

### Test 2: Frequent Locations in Setup Wizard
**Location**: First-time app launch → Setup Wizard → Frequent Locations step

- [ ] Locations page displays optional message
- [ ] FAB button visible
- [ ] Same autocomplete functionality as Test 1
- [ ] Can add multiple locations
- [ ] Can skip this step and continue setup
- [ ] Locations saved during setup appear in Settings later

### Test 3: Using Frequent Location for Walk
**Location**: Dashboard → Start Walk

- [ ] Saved frequent locations appear in selection list
- [ ] Click a frequent location → Auto-fills destination field
- [ ] GPS coordinates populated from saved location
- [ ] Start Walk button works

### Test 4: Walk Deduplication
**Location**: Dashboard → Start Walk → End Walk → History

- [ ] Start a walk with valid destination
- [ ] Walk for 10+ seconds to generate data
- [ ] End walk (press end button)
- [ ] Check **Walk History** tab
- [ ] **CRITICAL**: Should show exactly ONE walk entry (not two)
- [ ] Verify duration, destination, and data match
- [ ] Repeat 3 times with different destinations
- [ ] All walks should appear once in history

### Test 5: Modern UI/UX
**Location**: Settings → Frequent Locations list

- [ ] Location cards have rounded corners (16dp radius)
- [ ] Cards have subtle shadow beneath them
- [ ] Cards have thin stroke border
- [ ] Click card → Ripple effect appears
- [ ] Location icon has colored background container
- [ ] Icon color is primary color
- [ ] Card background is light (surface color)
- [ ] Text contrast is good and readable
- [ ] Address shows up to 2 lines (no truncation for reasonable addresses)
- [ ] Spacing between cards is balanced (8dp)

### Test 6: GPS Bias (Optional, location permission granted)
**Setup**: Allow location permission in app

- [ ] When adding location in GPS-enabled area
- [ ] Search for "pizza" → Results show nearby pizzerias first
- [ ] Suggestions are biased to your current location
- [ ] GPS coordinates captured when saving

### Test 7: GPS Bias Fallback (no location permission)
**Setup**: Deny location permission

- [ ] When adding location without GPS permission
- [ ] Search for "London" → Still shows results
- [ ] Results are global (not biased to nearby)
- [ ] No GPS coordinates saved (0,0 used as fallback)

### Test 8: Autocomplete Edge Cases
- [ ] Search with less than 3 characters → No suggestions shown
- [ ] Search with exactly 3 characters → Suggestions appear
- [ ] Type very long location name → No crashes
- [ ] Clear text field → Suggestions disappear
- [ ] Select suggestion → Dialog accepts it without refetching
- [ ] No internet → Graceful error or empty suggestions

---

## Performance Tests

### Test 9: Autocomplete Performance
- [ ] Type location quickly → No lag or jank
- [ ] Multiple searches in sequence → All work smoothly
- [ ] Switch between add/edit dialogs → No memory issues
- [ ] Open/close dialogs 10x → App remains responsive

### Test 10: Memory & Resources
- [ ] Open Frequent Locations screen
- [ ] Monitor RAM usage (Settings → Apps → WAY → Memory)
- [ ] Navigate away and back 5x
- [ ] RAM should not increase significantly
- [ ] No memory leak warnings in Logcat

---

## Integration Tests

### Test 11: Data Persistence
- [ ] Add 5 frequent locations
- [ ] Force-stop app (Settings → Apps → WAY → Stop)
- [ ] Reopen app
- [ ] All 5 locations still present

### Test 12: Setup Wizard Flow
- [ ] Uninstall app
- [ ] Clear app data
- [ ] Reinstall
- [ ] Go through setup wizard
- [ ] Add locations in wizard
- [ ] Complete setup
- [ ] Open Settings → Frequent Locations
- [ ] Locations from wizard are present

### Test 13: Walk Workflow
- [ ] Complete full walk workflow:
  1. Add frequent location in Settings
  2. Go to Start Walk
  3. Select frequent location from list
  4. Confirm destination
  5. Walk 20+ seconds
  6. End walk
- [ ] Check history → Walk present exactly once
- [ ] Verify destination name matches selected location

---

## Bug/Issue Tracker

| Issue # | Description | Status | Notes |
|---------|-------------|--------|-------|
| PH4-001 | Autocomplete doesn't show | ⏳ | Check API key |
| PH4-002 | Duplicates in history | ✅ | FIXED |
| PH4-003 | UI looks dated | ✅ | FIXED |
| PH4-004 | No place suggestions | ⏳ | Check internet |

---

## Known Limitations

1. **API Rate Limiting**: Geoapify free tier has 3,000 requests/day limit
   - Mitigation: App uses debounce (400ms) to prevent rapid requests
   - Estimated usage: 10-50 searches per user per day

2. **Offline Mode**: Autocomplete requires internet connection
   - Mitigation: Fallback to manual entry works without internet

3. **GPS Bias**: Only works if location permission granted
   - Mitigation: Gracefully falls back to global search results

---

## Reporting Issues

If you encounter problems during testing:

1. **Describe**: What did you do and what happened?
2. **Reproduce**: Can you do it again consistently?
3. **Evidence**: Screenshot or error message
4. **Device**: Phone model, Android version, app version

Format for Discord/Issue Report:
```
Title: [Phase 4] Brief description

Steps to reproduce:
1. ...
2. ...
3. ...

Expected:
...

Actual:
...

Device: Samsung Galaxy S10, Android 11, WAY v1.0.0
```

---

## Success Metrics

- [ ] 0 crashes related to Geoapify
- [ ] Walk history shows each walk exactly once
- [ ] Autocomplete response time < 1 second
- [ ] UI feels modern and polished
- [ ] All 13 test categories pass
- [ ] No memory leaks detected

---

## Next Steps After Phase 4

1. **Phase 5**: Improve safety trigger detection
   - Better fall detection algorithm
   - More accurate speed detection
   - Configurable inactivity timeouts

2. **Phase 6**: Enhanced location features
   - Offline map caching
   - Favorite locations quick-access
   - Location-based alerts

3. **Phase 7**: Performance & Battery
   - Optimize sensor usage
   - Battery drain reduction
   - Background task optimization



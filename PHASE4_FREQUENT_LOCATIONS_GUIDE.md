# Phase 4: Frequent Locations with Geoapify API - Complete Guide

## Overview
This guide explains how to use Geoapify API for location search in both the **Frequent Locations** settings screen and the **Setup Wizard**.

## What's New (Phase 4 Updates)

### ✅ Geoapify Autocomplete for Frequent Locations
- **FrequentLocationsFragment.kt**: Added autocomplete search when adding/editing locations
- **SetupLocationsFragment.kt**: Added autocomplete search during onboarding
- Users can now:
  - Search for locations by name or address
  - See real-time suggestions as they type
  - Auto-populate location names and addresses from Geoapify
  - Use GPS coordinates as a bias for "nearby" suggestions

### ✅ Enhanced UI/UX
- **Modern card design** with better visual hierarchy
- **Improved spacing** and typography
- **Location icon container** with accent background color
- **Ripple effect** on card click for better feedback
- **Better text truncation** for long addresses

### ✅ Walk Duplication Prevention
- Repository correctly checks if a walk session already has an ID before creating a new document
- Prevents duplicate walk records in Firestore
- Caches walks locally as failsafe

---

## Using Geoapify API in Frequent Locations

### Step 1: Verify Geoapify Configuration

Check if your `local.properties` file has the API key:

```properties
GEOAPIFY_API_KEY=3666750b67f448579f819336f91be739
```

### Step 2: Add a Frequent Location

1. Open **Settings** → **Frequent Locations**
2. Tap the **+ (FAB)** button
3. In the dialog:
   - Type location name (e.g., "Office")
   - Start typing address (e.g., "New York") - Geoapify suggestions appear
   - Select from dropdown OR type manually
   - Current GPS coordinates are captured if location permission is granted
4. Tap **Add** to save

### Step 3: Edit an Existing Location

1. Long-press a location card
2. Select **Edit** from popup menu
3. Modify name or address (with autocomplete support)
4. Update GPS by allowing location permission
5. Tap **Save**

### Step 4: Delete a Location

1. Long-press a location card
2. Select **Delete** from popup menu
3. Confirm deletion

---

## Geoapify Autocomplete Features

### Real-Time Search
- Search starts after typing **3+ characters**
- Suggestions update with **400ms debounce** to avoid excessive requests
- Shows place name, address, and coordinates

### GPS Bias
- If location permission is granted, current GPS is used to bias results
- Closer results rank higher in suggestions
- Works globally if GPS is unavailable

### Selected Place Information
When you select a place from suggestions:
- **Name**: Auto-fills location name field
- **Address**: Auto-fills address field
- **Coordinates**: Extracted and saved with location record

---

## During Setup Wizard (First-Time Onboarding)

### Step 1: Grant Location Permission
When prompted, allow location access to enable GPS bias for nearby suggestions.

### Step 2: Add Frequent Locations Step
- This step is **optional** but recommended
- Add your home, office, gym, etc. with Geoapify search
- You can skip if you prefer to add later from Settings

### Step 3: Use These Locations Later
Once setup is complete:
- Open **Start Walk** → **Select Destination** → **Frequent Locations**
- Tap any saved location to start a walk there

---

## API Request Limits

Geoapify offers **generous free tier**:
- **3,000 requests/day** (free plan)
- Suitable for most users
- Check Geoapify dashboard at https://myprojects.geoapify.com for usage

---

## Troubleshooting

### Problem: No suggestions appear
**Solution**: 
- Verify `GEOAPIFY_API_KEY` is set in `local.properties`
- Check if you typed at least 3 characters
- Ensure internet connection is active
- Check API key has **Autocomplete** permission enabled

### Problem: Suggestions are not localized
**Solution**:
- If GPS unavailable, searches are global
- Try allowing location permission
- Try searching with country name (e.g., "Paris, France")

### Problem: GPS coordinates not saving with location
**Solution**:
- Geoapify coordinates are used if manual GPS capture fails
- Move outdoors and retry with location permission enabled
- Check if "Location" permission shows "Allowed" in Settings

---

## Architecture

### FrequentLocationsFragment.kt
```kotlin
// Setup autocomplete when adding location
setupSearchAutocomplete(etAddress, currentGps) { place ->
    etName.setText(place.name)
    etAddress.setText(place.fullAddress)
}

// Search with debounce
searchPlaces(query, gpsBias) // 400ms delay
```

### FrequentLocationsViewModel.kt
```kotlin
// Resolve coordinates using Geoapify
private suspend fun resolveLatLng(
    name: String,
    address: String,
    fallback: Pair<Double, Double>
): Pair<Double, Double> {
    val place = GeoapifyService.autocomplete(query).firstOrNull()
    return place?.let { place.latitude to place.longitude } ?: fallback
}
```

---

## Manual Setup Steps Required

None additional! The Geoapify API key is already configured from `local.properties`.

If you get errors about missing API key:
1. Open `local.properties` in project root
2. Verify `GEOAPIFY_API_KEY=your_key_here`
3. Sync Gradle and rebuild

---

## Next Steps

- **Phase 5**: Improve trigger detection accuracy (fall detection, speed detection)
- **Phase 6**: Add offline maps and caching
- **Phase 7**: Performance optimization and battery management



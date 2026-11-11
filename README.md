# Aura Music — Offline Android Music App

A modern, fast, and offline music player built for Android using Kotlin and Jetpack Compose, designed around Apple Human Interface Guidelines and top audio player benchmarks.

## 📦 APK Deliverables
The APK files are located directly in the root of this project:
- **`AuraMusic-Release.apk`** (11.4 MB) — Recommended: Optimized, shrinked, signed release build.
- **`AuraMusic-Debug.apk`** (16.5 MB) — Debug build with logging enabled.

You can install either file directly onto your Android device:
```bash
adb install -r AuraMusic-Release.apk
```

---

## 🌟 Key Features

1. **Apple Design Theory & Fluid Spring Physics**
   - Pure OLED dark mode (`#000000`) and clean iOS light mode.
   - Dynamic fluid background palettes extracted in real-time from album art using AndroidX `Palette`.
   - Artwork scales dynamically with spring physics: expands on play, shrinks subtly on pause.
   - Frosted glass bottom mini-player bar.

2. **Strict Content Isolation ("No Mingling")**
   - **Voice Recordings Vault**: Voice memos, dictaphone, and sound recorder files are strictly isolated from normal music.
   - **Call Records Vault**: Phone call recordings are kept in their own dedicated screen.
   - **Recycle Bin**: Soft-deleted files are moved here and can be restored with 1 tap or emptied permanently.
   - **Find Duplicates**: Scans library, calculates wasted MBs, and cleans duplicate audio copies with 1 tap while keeping the best copy.

3. **Home Screen & Navigation**
   - **Top 5 Recently Played**: Horizontal Apple Music cards carousel with artwork and play badges.
   - **Segmented Control Tabs**: All Tracks, Favorites, and Custom Albums.
   - **Fast A-Z Alphabet Scroller ("abcd track")**: Right-rail alphabet touch index with floating magnifier bubble.
   - **Auto-Search Everywhere**: Live instant search across songs, artists, albums, and paths.

4. **Custom Albums with Custom Photos**
   - Create custom albums/playlists.
   - Select any custom image from the device gallery as the album cover.

5. **Multi-Select & QuickShare**
   - Select All toggle.
   - QuickShare selected songs or full albums via Android native Share Sheet (FileProvider).

6. **Full-Screen Now Playing & Sleep Timer**
   - Dynamic artwork mesh background.
   - Time scrubber with remaining countdown `-mm:ss`.
   - Sleep timer with 15m, 30m, 45m, 60m, custom time, and smooth 3-second audio fade-out.
   - Repeat (Off, All, One) and Shuffle modes.
   - Background audio playback with lock screen media controls and notification actions.

---

## 🛠️ Building from Source

```powershell
# Build Release APK
.\gradlew assembleRelease

# Build Debug APK
.\gradlew assembleDebug
```
Built using Gradle 8.5, Android SDK 34, Adoptium JDK 17, and Kotlin 1.9.22.

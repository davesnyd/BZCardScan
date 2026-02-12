# BZ Card Scan — Build & Install Guide

## Quick Install (No Build Required)

If you just want to install the app on your phone:

1. On your Android phone, go to the [latest release](https://github.com/davesnyd/BZCardScan/releases/latest)
2. Download **app-debug.apk**
3. Open the downloaded file
4. If prompted, allow **"Install from unknown sources"** for your browser or file manager
5. Tap **Install**

That's it. The app requires Android 8.0 (Oreo) or newer.

To verify your download, compare the SHA256 hash listed on the release page with:
```bash
sha256sum app-debug.apk
```

---

## Building from Source

The rest of this guide is for developers who want to build the app themselves.

## Prerequisites

### Android Studio

Android Studio is required to build the project. It is free and available from Google.

**Install via snap (Linux):**
```bash
sudo snap install android-studio --classic
```

**Or download directly:**
https://developer.android.com/studio

Android Studio includes the Android SDK, build tools, and an emulator. The first launch will prompt you to download SDK components.

### System Requirements

- **OS**: Windows, macOS, or Linux
- **RAM**: 8 GB minimum (16 GB recommended)
- **Disk**: ~10 GB for Android Studio + SDK
- **Java**: Bundled with Android Studio (JetBrains Runtime 21)

## Building the Project

### 1. Open the Project

1. Launch Android Studio
2. Click **Open** and navigate to the `BZCardScan` directory
3. If prompted about "Safe Mode" or "Trust", click **Trust Project**
4. Wait for the Gradle sync to complete (first time downloads dependencies and may take several minutes)

### 2. Verify Gradle JDK

If you see a "Gradle JDK" error:

1. Go to **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**
2. Set **Gradle JDK** to **JetBrains Runtime 21** (bundled with Android Studio)
3. Click **Apply > OK**

### 3. Build the APK

**From the menu:**
- Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**

**Or from the terminal:**
```bash
./gradlew assembleDebug
```

The debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## Installing on Your Phone

### Option A: Direct from Android Studio (Recommended)

This is the fastest method for development and testing.

**One-time phone setup:**

1. On your phone, go to **Settings > About Phone**
2. Tap **Build Number** 7 times to enable Developer Options
3. Go to **Settings > System > Developer Options** (location varies by phone brand)
4. Enable **USB Debugging**

**Connect and run:**

1. Connect your phone to your computer via USB cable
2. On your phone, pull down the notification shade and tap the USB notification
3. Change USB mode to **File Transfer** (MTP)
4. If prompted on your phone, tap **Allow USB Debugging** (check "Always allow from this computer")
5. In Android Studio, your phone should appear in the device dropdown (top toolbar, next to the Run button)
6. Select your phone and click the green **Run** button (or press Shift+F10)

Android Studio will build the app and install it directly on your phone.

### Option B: Transfer the APK Manually

If you prefer not to use USB debugging:

1. Build the APK (see above)
2. Transfer `app-debug.apk` to your phone via:
   - USB file transfer (connect phone, change USB mode to File Transfer, copy file)
   - Email the APK to yourself
   - Upload to Google Drive or other cloud storage
3. On your phone, open the APK file
4. If prompted, allow **Install from unknown sources** for the app you used to open it (Files, Chrome, etc.)
5. Tap **Install**

### Option C: Install via ADB

If you have ADB configured:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|-----------|---------|
| `CAMERA` | Capturing photos of business cards |
| `GET_ACCOUNTS` | Listing Google accounts for contact export |

Both permissions are requested at runtime when needed.

## Troubleshooting

### Phone not showing in Android Studio

- Verify USB Debugging is enabled in Developer Options
- Try a different USB cable (some cables are charge-only)
- Change USB mode to File Transfer in the notification shade
- Check for an "Allow USB debugging?" prompt on your phone
- Run `adb devices` in Android Studio's terminal to check connectivity

### Gradle sync fails

- Ensure you have an internet connection (first sync downloads dependencies)
- Check that Gradle JDK is set to JetBrains Runtime 21
- Try **File > Invalidate Caches and Restart**
- If you see `ConcurrentModificationException`, just re-sync — it's a known Android Studio bug

### Build fails after updating Android Studio

- Accept any prompted updates via **File > Sync Project with Gradle Files**
- If AGP (Android Gradle Plugin) update is recommended, use the **AGP Upgrade Assistant**

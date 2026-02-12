# BZ Card Scan

An Android app that scans business cards using your phone's camera, extracts contact information with OCR, and saves it to your contacts.

## Features

- **Camera scanning** — Point your camera at a business card and tap to capture
- **On-device OCR** — Text recognition runs locally using Google ML Kit (no internet required)
- **Smart field parsing** — Automatically identifies name, job title, company, phone numbers, email, website, and address from scanned text
- **Editable results** — Review and correct extracted fields before saving
- **Tap-to-assign** — Tap any line of raw OCR text to manually assign it to a field
- **Local database** — Scanned cards are saved to a Room (SQLite) database on your device
- **Google Contacts export** — Export saved cards directly to your Google Contacts with account selection memory
- **Crop-to-frame** — Only the area inside the on-screen guide rectangle is processed for OCR

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Camera**: CameraX
- **OCR**: Google ML Kit Text Recognition
- **Database**: Room (SQLite)
- **Navigation**: Jetpack Navigation Compose
- **Min SDK**: Android 8.0 (API 26)

## Documentation

- [User Guide](docs/USER_GUIDE.md) — How to use the app
- [Build & Install Guide](docs/BUILD_INSTALL.md) — How to build from source and install on your phone

## License

This project is privately maintained.

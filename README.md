# TextGrab

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Android](https://img.shields.io/badge/platform-Android-green.svg?logo=android)](https://www.android.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Liberapay](https://img.shields.io/liberapay/receives/kys0ff.svg?logo=liberapay)](https://liberapay.com/kys0ff/donate)

TextGrab is a modern, Free and Open Source Software (FOSS) Android application designed to make screen content interactive. It allows users to extract and copy text from almost any app or screen with advanced multi-selection support.

## Core Features

- **Accessibility-Based Extraction**: The primary engine uses Android's Accessibility Services to directly read text nodes from the screen, ensuring high accuracy and low resource usage.
- **Multi-Selection Support**: Select multiple blocks of text simultaneously, combine them, or copy them individually.
- **OCR Fallback**: For cases where text is displayed as an image or within a non-interactive element, TextGrab includes a powerful OCR engine (Tesseract) that can "see" text via screen capture.
- **On-Demand OCR Packages**: To keep the app lightweight, **no OCR models are pre-installed**. Users can choose exactly which languages they need from the **OCR Package Screen**.
- **Modern UI**: A clean, responsive interface built with Jetpack Compose and Material 3.
- **Quick Settings Tile**: Start a scan instantly from your notification shade.

## Permissions

To provide its core functionality, TextGrab requires the following permissions:

- **Accessibility Service**: Required to read the text nodes on your screen and display the selection overlay.
- **Display Over Other Apps**: Allows the app to show the selection interface on top of whatever you are currently doing.
- **Screen Recording (Media Projection)**: Used exclusively for the OCR fallback engine to capture a screenshot for text recognition.
- **Internet Access**: Needed only for downloading OCR language data packages on demand.
- **Notifications**: Used to manage the foreground services required for screen capture and data synchronization.

## Tech Stack

- **Kotlin**: The primary language for development.
- **Jetpack Compose**: Declarative UI toolkit for modern layouts.
- **Tesseract4Android**: Robust OCR engine for the fallback mechanism.
- **Koin**: Lightweight dependency injection.
- **Voyager**: Pragmatic navigation for Compose.
- **DataStore**: Secure persistence for settings and history.

## Getting Started

### Prerequisites

- Android 6.0 (API 23) or higher.
- Accessibility Service must be enabled for TextGrab.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/kys0adam/TextGrab.git
   ```
2. Open the project in Android Studio.
3. Build and run the app on your device.

## Contributing

Contributions are welcome! Whether it's a bug report, a feature request, or a translation, your help is appreciated.

## Donate

If you find this project useful, consider supporting development via Liberapay:

[![Donate with Liberapay](https://liberapay.com/assets/widgets/donate.svg)](https://liberapay.com/kys0adam/donate)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-16

### Added
- Initial release of TextGrab (FOSS).
- Support for Android 6.0+ (API 23+).
- Dual-engine architecture:
    - **Primary**: Accessibility-based text extraction for high accuracy.
    - **Fallback**: Tesseract OCR engine for reading text from images/non-interactive areas.
- Multi-selection support for copying multiple text fragments at once.
- OCR Package management: Lightweight app with 0 pre-installed models; download languages as needed (English, Arabic, French, German, Chinese, Japanese, Korean).
- UI localization for English, Arabic, German, Spanish, and French.
- Modern UI built with Jetpack Compose and Material 3.
- Quick Settings Tile for instant scanning.

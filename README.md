# Retro iPod Classic Music Player for Android

A high-fidelity retro iPod Classic music player experience built for modern Android devices using **Jetpack Compose**, **Material 3**, and **AndroidX Media3**.

![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02.00-blue?logo=jetpackcompose)
![MinSDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange)
![TargetSDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-teal)

---

## ✨ Features

- 🎧 **Authentic iPod Classic Experience**:
  - Classic dual-tone LCD display with Helvetica and SF Pro typography.
  - Authentic split-screen layout with dynamic right-panel previews (Now Playing, Cover Art, and dynamic showcase).
  - Multiple chassis skin themes: **Silver Classic**, **Stealth Black**, **U2 Special Edition**, and **Product (RED)**.
  - Interactive retro chassis stickers with drag, pinch-to-scale, rotate, and persistent customization.

- 🎛️ **Rotary Click Wheel Simulation**:
  - Full circular angular gesture tracking with velocity-based inertial momentum scrolling.
  - Crisp haptic tick feedback synchronized with list navigation.
  - Dedicated tactile physical-style buttons: `MENU`, `PREVIOUS`, `NEXT`, `PLAY/PAUSE`, and center `SELECT`.

- 🎵 **High-Fidelity Audio & Bit-Perfect Playback**:
  - Powered by **AndroidX Media3 ExoPlayer** with background playback service and media notification controls.
  - Real-time audio stream metrics reporting sample rate, bit depth, and bitrate.
  - External USB DAC detection and bit-perfect playback support with Google Oboe C++ audio pipeline integration.

- 🎤 **Advanced Synchronized Lyrics Engine**:
  - Supports standard and enhanced syllable-level `.lrc` files.
  - Automatic extraction of embedded ID3 USLT/SYLT tags and Vorbis comments from local audio files.
  - **Two display modes**:
    - **Classic iPod**: Nostalgic monochrome text with smooth spring auto-scrolling.
    - **Modern Fluid (Aurora)**: Dynamic mesh gradient background dynamically extracted from album art, blurred backdrop, and fluid spring animations.

- 📁 **Comprehensive Music Library & Playlists**:
  - Background `MediaStore` audio scanner with Room database persistence.
  - Hierarchical navigation: Artists, Albums, Songs, Genres, Compilations, and Search.
  - Fast search with ranking and alphabetical quick navigation.
  - Full `.m3u` / `.m3u8` playlist import and export.

---

## 🛠️ Tech Stack & Architecture

- **UI Layer**: Jetpack Compose, Material 3, Compose Navigation, Coil for image loading, Palette for dynamic color extraction.
- **Audio & Media**: AndroidX Media3 (ExoPlayer 1.2.1), MediaSession, C++ Oboe Audio HAL integration.
- **Persistence**: AndroidX Room (SQLite database with multi-table cross-reference queries), Jetpack DataStore Preferences.
- **Concurrency**: Kotlin Coroutines, StateFlow, SharedFlow.
- **Architecture**: Clean Architecture / MVVM with reactive unidirectional data flow.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or JDK 21
- Android SDK 34 (API 34)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/mulkihafizh/ipod.git
cd ipod

# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

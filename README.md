# Whistle Detection for Wear OS

## Overview

This project implements a whistle detection system for Wear OS smartwatches, designed to assist individuals who are deaf or hard of hearing in sports activities. The system analyzes incoming audio, detects whistle frequencies using Fast Fourier Transform (FFT), and provides haptic feedback through vibration.

## Features

- Real-time and offline whistle detection.
- Uses FFT for frequency analysis.
- Employs MusicG library for signal processing.
- Operates as a standalone Wear OS app without phone pairing.
- Runs in the background using a Foreground Service.
- Optimized for power efficiency.

## Installation

### Prerequisites

- Android Studio installed.
- Wear OS emulator or a physical smartwatch.
- Java 8+ configured for Android development.

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/mekkiahmedi/WhistleDetection-WearOS.git
   cd WhistleDetection-WearOS
   ```
2. Open the project in **Android Studio**.
3. Sync Gradle dependencies.
4. Build and deploy the application to a Wear OS device.

## How It Works

1. **Audio Capture**: Continuously records audio using the Wear OS microphone.
2. **FFT Processing**: Converts the signal to the frequency domain.
3. **Energy Thresholding**: Filters background noise.
4. **Pattern Matching**: Uses cross-correlation to validate whistles.
5. **Haptic Feedback**: Vibrates when a whistle is detected.



## References

- [Fast Fourier Transform (FFT)](https://en.wikipedia.org/wiki/Fast_Fourier_transform)
- [MusicG Library](https://github.com/loisaidasam/musicg) (visited on 11 January 2025)
- [Wear OS Development](https://developer.android.com/training/wearables)

## Citation

Makki Abdelmagid Abdelhalim. (2025). Whistle Detection for Wear OS. GitHub. Retrieved from https://github.com/mekkiahmedi/WhistleDetection-WearOS


This project is licensed under the MIT License.


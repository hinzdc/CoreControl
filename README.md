# Core Control — SARGAVERSE

> **High-Precision Android Root CPU Core Hotplug Utility**  
> Published by **SARGAVERSE** | Package: `com.sargaverse.corecontrol`

---

## 1. Project Title & Introduction

**Core Control** is an advanced, root-enabled Android system utility designed for developers, kernel enthusiasts, and power users who require deterministic, real-time control over CPU core online/offline states. Built natively in Kotlin with Jetpack Compose following Clean Architecture principles, Core Control bypasses high-level OS abstractions and communicates directly with the Linux kernel via `/sys/devices/system/cpu/` interfaces through KernelSU or Magisk `su`.

---

## 2. Application Overview

Modern mobile System-on-Chips (SoCs) employ complex multi-cluster architectures (e.g., big.LITTLE, DynamIQ) managed by thermal and power schedulers. While standard Android frameworks lack granular APIs to power-down individual cores on demand, Core Control provides a responsive, compact 4×2 technical dashboard allowing users to:
* Inspect real hardware online/offline status directly from sysfs.
* Toggle individual CPU cores on or off without affecting neighbouring cores.
* Perform safe bulk activations or deactivations.
* Automatically restore all eligible CPU cores to ONLINE upon system reboot without persisting unstable offline profiles.

---

## 3. Main Features

* **True Direct Core Toggling**: Tap any CPU card to transition that specific core between ONLINE and OFFLINE.
* **CPU0 Hardware Protection**: CPU0 is hardcoded as protected and cannot be disabled under any circumstances to prevent kernel panics.
* **Compact 4×2 Responsive Grid**: Formatted specifically so all 8 CPU cores fit cleanly on a single screen without vertical scrolling.
* **Non-Glow Technical Theme**: Anti-AI-slop design aesthetic utilizing deep charcoal graphite, matte status containers, and high-contrast typography.
* **Separated Bulk Controls**: Independent `ALL ON` and `ALL OFF` bulk operations with pre-execution safety confirmations.
* **Automated Boot Restoration**: Listens for Android `BOOT_COMPLETED` events and restores eligible offline cores to ONLINE.
* **Zero Offline Profile Persistence**: Manually disabled core states are never saved to persistent storage, guaranteeing clean reboot states.
* **Audit History & Telemetry**: Full in-memory operation logs recording command execution latency, target core IDs, and sysfs readback verification.

---

## 4. CPU Individual Control Explanation

### Absolute Core Control Rule
In Core Control, **one tap must only control the CPU core that was tapped**.

* **Tap CPU 1 OFF** &rarr; Only CPU 1 transitions to offline.
* **Tap CPU 3 OFF** &rarr; Only CPU 3 transitions to offline.
* **Tap CPU 3 ON** &rarr; Only CPU 3 transitions to online.
* **Every other CPU retains its previous state**.

Each card invocation calls `cpuRepository.toggleCpuCore(cpuId, targetOnline)`. Core Control never executes broad shell wildcards or global offline triggers during individual card interactions.

### Execution Cycle
1. User taps a controllable CPU card.
2. The engine reads the current physical state of `/sys/devices/system/cpu/cpuN/online`.
3. Validates that the core is hotpluggable and not protected (`cpuId != 0`).
4. Executes root command via `su`:
   ```bash
   su -c 'echo 0 > /sys/devices/system/cpu/cpu3/online' # Disable
   su -c 'echo 1 > /sys/devices/system/cpu/cpu3/online' # Enable
   ```
5. Reads `/sys/devices/system/cpu/cpu3/online` again to verify the actual hardware status.
6. Updates the UI based strictly on the verified hardware readback.

---

## 5. Bulk ALL ON / ALL OFF Explanation

Bulk operations are completely separated from individual card controls to avoid unintended cascades:

* **ALL ON**:
  * Scans all cores from 1 to $N$.
  * Filters for offline cores supporting hotplug.
  * Individually triggers state transition to `1` with readback verification.
* **ALL OFF**:
  * Disables eligible secondary cores individually to minimize power consumption.
  * **Safety Guarantees**:
    * CPU0 is unconditionally protected.
    * At least one core per detected cluster is kept online where topology allows to maintain responsive scheduler dispatch.
    * Never leaves the system without an active execution core.

---

## 6. KernelSU & Root Requirements

Core Control requires an active root provider with standard `su` binary execution capabilities:
* **KernelSU**: Fully supported and recommended (native kernel-level root).
* **Magisk**: Fully supported via standard `su -c` execution.
* **APatch**: Fully supported via standard `su` bridge.

### Root Verification Engine
The app tests root access by verifying command exit codes and inspecting process privileges (`id -u == 0`). If root authorization is denied or unavailable, the dashboard enters a read-only safety mode where status cards and controls are disabled with clear diagnostic messaging.

---

## 7. CPU Hotplug Compatibility Notes

Android devices interface with CPU hotplug via Linux sysfs:
* Path: `/sys/devices/system/cpu/cpuN/`
* Control node: `cpuN/online` (`1` = Online, `0` = Offline).

### Vendor-Specific Behaviors & Hardware Locks
1. **CPU0**: Standard Linux kernel configurations do not expose an `online` file for `cpu0` (it is the primary boot CPU). Core Control marks CPU0 as `PROTECTED`.
2. **Missing `online` node**: If a core lacks an `online` node, hotplug is not supported by the kernel for that core. Core Control marks it as `UNSUPPORTED` and disables click interactions.
3. **Vendor Thermal Engines**: OEM daemons (such as Qualcomm `thermal-engine`, MediaTek `perfd`, or vendor powerHAL) may periodically override sysfs hotplug values under heavy load or temperature throttling. Core Control always reports the **real verified sysfs status** rather than assuming cached success.

---

## 8. Boot Restore Functionality

To prevent accidental soft-locks or degraded multi-threaded performance after testing, Core Control features an automated boot restoration service:

* **Receiver**: `BootCompletedReceiver` listens for:
  * `android.intent.action.BOOT_COMPLETED`
  * `android.intent.action.LOCKED_BOOT_COMPLETED`
  * `android.intent.action.QUICKBOOT_POWERON`
* **Workflow**:
  1. Awaits initial system stabilization delay (~2500ms).
  2. Verifies root authorization through KernelSU/su.
  3. Scans available CPU cores via sysfs.
  4. Identifies eligible offline cores (`!isOnline && hotplugSupported && cpuId != 0`).
  5. Sequentially activates each offline core with readback verification and controlled retries.
  6. Logs the operation outcome into the audit history.
* **Configurable**: Users can toggle **"Restore All Cores on Boot"** in Settings (Default: **ENABLED**). When disabled, no restoration takes place.

---

## 9. Technical Architecture

Core Control is structured under **Clean Architecture** with explicit separation of concerns:

```
com.sargaverse.corecontrol
├── data
│   ├── repository
│   │   ├── CpuRepositoryImpl.kt       # Sysfs reader, hotplug mutator, Mutex lock
│   │   ├── RootRepositoryImpl.kt      # Process executor, KernelSU verification
│   │   └── OperationLogRepositoryImpl.kt
│   └── settings
│       └── SettingsRepositoryImpl.kt  # DataStore Preferences (restore on boot)
├── domain
│   ├── model
│   │   ├── CpuCore.kt                 # Capability states: ONLINE, OFFLINE, PROTECTED...
│   │   ├── CpuSummary.kt
│   │   └── RootStatus.kt
│   ├── repository
│   │   ├── CpuRepository.kt
│   │   ├── RootRepository.kt
│   │   └── SettingsRepository.kt
│   └── usecase
│       ├── BulkCpuControlUseCase.kt   # Safe multi-core orchestration
│       ├── DetectCpuTopologyUseCase.kt
│       ├── RestoreCoresOnBootUseCase.kt
│       └── ToggleCpuCoreUseCase.kt    # Single-core isolated mutator
├── presentation
│   ├── components
│   │   ├── BulkControlsSection.kt
│   │   ├── CpuCoreCard.kt             # Compact clickable rectangular card
│   │   ├── CpuHeaderSection.kt
│   │   ├── CpuSummarySection.kt
│   │   └── OperationHistorySection.kt
│   ├── dashboard
│   │   ├── DashboardScreen.kt         # 4x2 Responsive Grid
│   │   └── DashboardViewModel.kt
│   ├── settings
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme
│       ├── Color.kt                   # Minimalist graphite/charcoal palette
│       └── Theme.kt
└── receiver
    └── BootCompletedReceiver.kt       # Startup broadcast receiver
```

---

## 10. Technology Stack

* **Language**: Kotlin 2.0+
* **UI Framework**: Jetpack Compose (Material 3)
* **Asynchronous Runtime**: Kotlin Coroutines & Flow
* **Persistence**: Jetpack DataStore Preferences
* **Root Execution**: Android Process / `su` pipe
* **Build System**: Gradle 8.10+ Kotlin DSL (`build.gradle.kts`)
* **Minimum SDK**: API 29 (Android 10)
* **Target / Compile SDK**: API 35 (Android 15)
* **Architecture**: ARM64 (`arm64-v8a`) compatibility

---

## 11. Project Directory Structure

```
CoreControl/
├── app/
│   ├── build.gradle.kts               # Android application build script
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    # Permissions, receivers, launcher
│       │   ├── java/com/sargaverse/corecontrol/
│       │   └── res/
│       └── test/                      # JVM Safety & Isolation unit tests
├── gradle/
│   └── libs.versions.toml             # Version catalog dependencies
├── dist/                              # Version-date formatted APK distributions
├── scripts/
├── build_release.sh                   # Automated test, assemble, distribute script
├── keystore.properties.example        # Release signing configuration template
└── README.md
```

---

## 12. Build Instructions

### Prerequisites
* JDK 17 (OpenJDK 17)
* Android SDK Platform 35 & Build-Tools
* Linux / macOS / Android Termux environment

### Command-Line Build
To run tests and compile APKs manually:

```bash
# Clean project
./gradlew clean

# Run safety unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK (signed if configured, unsigned fallback)
./gradlew assembleRelease

# Package into dist/ with version-date naming
./gradlew distributeApks
```

### All-in-One Automated Release Script
Run the automated pipeline script:
```bash
chmod +x build_release.sh
./build_release.sh
```

---

## 13. APK Installation Instructions

### Direct Installation on Rooted Device
```bash
# Install Debug APK
adb install -r dist/CoreControl-v1.0.0-debug-YYYY-MM-DD.apk

# Install Release APK
adb install -r dist/CoreControl-v1.0.0-release-YYYY-MM-DD.apk
```

### First-Launch Steps
1. Launch **Core Control** from the app drawer.
2. When prompted by KernelSU / Magisk, grant **Superuser** privileges.
3. The dashboard header will display `Root Access: Granted (KernelSU / SU)`.

---

## 14. Release Signing Instructions

To produce cryptographically signed release APKs suitable for distribution:

### 1. Generate Keystore
Generate a private RSA keystore using `keytool`:
```bash
keytool -genkeypair -v \
  -keystore ~/.android/sargaverse-release.jks \
  -alias sargaverse \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=SARGAVERSE, OU=Engineering, O=SARGAVERSE, L=Jakarta, ST=DKI, C=ID"
```

### 2. Configure Credentials
Choose either method:

#### Option A: `keystore.properties` (Recommended for Local Builds)
Create a `keystore.properties` file in the project root (this file is ignored by `.gitignore`):
```properties
storeFile=/data/data/com.termux/files/home/.android/sargaverse-release.jks
storePassword=YourKeystorePassword
keyAlias=sargaverse
keyPassword=YourKeyPassword
```

#### Option B: Environment Variables (Recommended for CI/CD)
Set the following environment variables:
```bash
export KEYSTORE_PATH="/path/to/sargaverse-release.jks"
export KEYSTORE_PASSWORD="YourKeystorePassword"
export KEY_ALIAS="sargaverse"
export KEY_PASSWORD="YourKeyPassword"
```

### 3. Verify Signature
Verify the resulting APK signature using `apksigner`:
```bash
apksigner verify --verbose dist/CoreControl-v1.0.0-release-YYYY-MM-DD.apk
```

---

## 15. Versioning & APK Naming Conventions

* **Version Code**: Integer incremented per release (current: `1`).
* **Version Name**: Semantic versioning `MAJOR.MINOR.PATCH` (current: `1.0.0`).
* **Distribution Filename Format**:
  ```
  CoreControl-v{versionName}-{buildType}-{YYYY-MM-DD}.apk
  ```
  Examples:
  * `dist/CoreControl-v1.0.0-debug-2026-11-19.apk`
  * `dist/CoreControl-v1.0.0-release-2026-11-19.apk`

---

## 16. GitHub Contribution & Workflow

1. Fork the repository and create a feature branch (`git checkout -b feature/my-feature`).
2. Adhere to Clean Architecture; do not couple domain logic with Android framework classes.
3. Ensure all unit tests pass: `./gradlew test`.
4. Commit your changes with descriptive messages:
   ```bash
   git commit -m "feat: improve thermal governor detection"
   ```
5. Never commit keystores, passwords, or distribution binaries (`dist/` is git-ignored).
6. Submit a Pull Request targeting the `main` branch.

---

## 17. Known Limitations

* **Kernel Support**: Sysfs core hotplugging is dependent on kernel configuration (`CONFIG_HOTPLUG_CPU=y`). Devices with locked kernels or monolithic architectures that disable CPU hotplug will display cores as `UNSUPPORTED`.
* **Vendor Daemon Overrides**: Certain SoC thermal management daemons (e.g., Qualcomm thermal-engine) aggressively force offline cores back online under load or forcibly disable cores when throttling.
* **Untested Hardware**: Tested primarily on ARM64 multi-cluster architectures. Behaviour on customized emulator kernels or non-standard architectures may vary.

---

## 18. Safety Warnings

> [!WARNING]
> **Kernel Stability**: Disabling too many cores during high system load may cause performance stutter or temporary audio dropouts.
> 
> **Never Attempt to Disable CPU0**: Core Control permanently protects CPU0. Modifying or bypassing this safety check can cause immediate kernel panics and spontaneous device reboots.

---

## 19. License

Copyright &copy; 2026 SARGAVERSE. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

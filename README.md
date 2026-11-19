<div align="center">

# Core Control

**Precision Linux Sysfs CPU Hotplug, Real-Time Frequency Telemetry & Cluster Intelligence for Rooted Android & Xiaomi HyperOS**

[![Publisher](https://img.shields.io/badge/Publisher-SARGAVERSE-00F0FF?style=for-the-badge)](https://github.com/hinzdc)
[![Platform](https://img.shields.io/badge/Platform-Android%2010%2B%20(Up%20to%20Android%2017)-30D158?style=for-the-badge)](https://developer.android.com)
[![Root](https://img.shields.io/badge/Root-KernelSU%20%7C%20APatch%20%7C%20Magisk-7C3AED?style=for-the-badge)](https://kernelsu.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material3-blue?style=for-the-badge)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20Architecture-FF6B6B?style=for-the-badge)](docs/ARCHITECTURE.md)

</div>

---

## Overview

**Core Control** is a hardware-level Android system utility engineered under the **SARGAVERSE** suite. It interfaces directly with the Linux kernel `/sys/devices/system/cpu/` sysfs subsystem, providing real-time CPU core telemetry, frequency monitoring, cluster topology diagnostics, and root-governed individual core hotplugging.

Unlike generic kernel managers that issue broad scripts or modify global governors, Core Control is built upon the **Absolute Core Control Rule**: every operation targets strictly and exclusively the selected CPU core, preserving all other cores in their exact hardware states.

---

## The Absolute Core Control Rule

> **ONE TAP MUST ONLY CONTROL THE CPU CORE THAT WAS TAPPED.**

- **Target Isolation**: Tapping CPU 3 OFF disables **only** CPU 3 (`echo 0 > /sys/devices/system/cpu/cpu3/online`). Every other core (CPU 0, 1, 2, 4, 5, 6, 7) retains its state untouched.
- **CPU0 Immutable Safety**: Core 0 is the primary system bootstrap processor and critical interrupt handler. It is locked into `PROTECTED` state and can never be disabled via the UI or bulk commands.
- **Hardware State Re-verification**: Success is never declared from shell return codes alone. Sysfs is re-read immediately to guarantee the kernel accepted the transition.
- **Mutex Serialization**: Operations are serialized across a thread-safe Kotlin `Mutex` to prevent concurrent sysfs collisions.

---

## System Architecture

Core Control is structured according to strict **Clean Architecture**:

```
com.sargaverse.corecontrol
├── data
│   ├── cpu        # SysfsCpuReader, CpuRepositoryImpl
│   ├── root       # RootEngine (KernelSU / SU executor, shell sanitization)
│   ├── settings   # DataStore preferences
│   └── history    # In-memory OperationLogRepositoryImpl
├── domain
│   ├── model      # CpuCore, CpuCapabilityState, CpuCluster, OperationResult
│   ├── repository # Interfaces: CpuRepository, RootRepository, SettingsRepository
│   └── usecase    # GetCpuCoresUseCase, ToggleCpuCoreUseCase, BulkCpuControlUseCase
└── presentation
    ├── dashboard  # DashboardViewModel, DashboardScreen, DashboardUiState
    ├── components # CpuCoreCard, SummarySection, BulkSection, SystemInfoSection
    ├── settings   # SettingsViewModel, SettingsScreen
    └── theme      # Technical Dark-first Cyberpunk palette, Typography, Theme
```

---

## Core Capability States

| State | Badge | Description |
|---|---|---|
| `ONLINE` | Emerald Green | Core active and processing kernel instructions. |
| `OFFLINE` | Slate Gray | Core powered down in deep sleep state. |
| `PROTECTED` | Amber | Critical system execution core (CPU0), locked active. |
| `UNSUPPORTED` | Gray | Core missing sysfs hotplug interface. |
| `ERROR` | Neon Red | SELinux denial or kernel hardware restriction. |

---

## Bulk Control Safeguards

- **ALL ON**: Activates eligible offline cores individually, verifying each core's online status.
- **ALL OFF**: Deactivates eligible secondary cores while preserving:
  1. **CPU0** (system anchor).
  2. **At least one execution core per detected cluster** (Efficiency, Performance, and Prime), preventing scheduler collapse.
  3. Interactive confirmation dialog before execution.

---

## Building from Source

Core Control utilizes the modern **Gradle Kotlin DSL**:

```bash
# Clean project
./gradlew clean

# Run unit test suite (Includes CPU0 protection & critical isolation tests)
./gradlew test

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

Generated APK Artifacts:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Hardware Telemetry & Test Verification

Tested on physical rooted hardware:
- **Device**: Xiaomi HyperOS 3 (`OS3.0.309.0.WPJMIXM`)
- **Platform**: MediaTek Dimensity (`mt6899`) ARM64
- **Root Provider**: KernelSU (`u:r:ksu:s0`)
- **CPU Configuration**: Octa-Core Tri-Cluster (4x Efficiency, 3x Performance, 1x Prime)

---

## License

Published under the **GNU General Public License v3.0** (GPL-3.0) by **SARGAVERSE**.

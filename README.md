# Glyph-Glance

**Glyph-Glance** is a privacy-first, on-device AI agent built exclusively for the **Nothing Phone**. It acts as a digital bouncer for your attention, filtering notification noise and visualizing message urgency through the Glyph Interface lights—allowing you to know *exactly* when to look and when to ignore without unlocking your device.

-----

## ⚡ The Problem: "Phantom Buzz" & Notification Fatigue

We check our phones 150 times a day, often for low-value interruptions.

  * **Split-Texters:** Friends who send 10 short messages ("Hey", "Are", "You", "There") trigger 10 separate buzzes.
  * **Context Blindness:** A standard vibration feels the same for a spam email as it does for a family emergency.
  * **Privacy Risks:** Most "smart" notification assistants require sending your private messages to the cloud for analysis.

## 💡 The Solution: Glyph-Glance

Glyph-Glance intercepts notifications and processes them locally using **Cactus Compute** (running a quantized LLM). It intelligently buffers rapid-fire messages, analyzes their semantic intent (Urgency vs. Spam), and maps them to distinct, glanceable light patterns on the back of your phone.

-----

## 🏗️ Architecture

The project is architected into three distinct, decoupled tracks to ensure separation of concerns between AI logic, system integration, and user experience.

### Part 1: The Brain (Core Logic & Data)

  * **Responsibility:** Pure logic, data persistence, and AI reasoning.
  * **Key Components:**
      * **Cactus Wrapper:** Manages the lifecycle of the local **Qwen3-0.6B** model.
      * **Rule Engine:** Matches incoming text against user-defined natural language rules (e.g., "Block everything unless it's Mom").
      * **Contact Profiling:** Maintains a local database of sender habits to identify "Split Texters".

### Part 2: The Body (System & Hardware)

  * **Responsibility:** Android OS integration and physical hardware control.
  * **Key Components:**
      * **Notification Listener:** A background service (`NotificationListenerService`) that intercepts system notifications.
      * **Buffer Engine:** A state machine that catches rapid-fire messages and holds them in a queue before processing, reducing notification spam.
      * **Glyph Manager:** Controls the **Nothing Glyph Interface** via the GDK, translating AI decisions into light patterns (e.g., Red Strobe for Urgency).

### Part 3: The Face (UI & Configuration)

  * **Responsibility:** User interaction and visualization.
  * **Key Components:**
      * **Live Terminal:** A "Matrix-style" scrolling log that visualizes the AI's decision-making process in real-time, building user trust.
      * **Natural Language Config:** A text input that allows users to program the AI using plain English, which is then converted to JSON rules.
      * **Permissions Wizard:** Guides users through granting sensitive Android permissions (Notification Access, Glyph Control).

-----

## 🛠️ Tech Stack

  * **Language:** Kotlin Multiplatform (KMP) targeting Android (Primary) and iOS (Logic only).
  * **UI Framework:** Jetpack Compose (Material 3).
  * **AI Engine:** [Cactus SDK](https://cactuscompute.com) running `Qwen3-0.6B-Instruct` (GGUF).
  * **Database:** Room (SQLite) for persistent rules and contact profiles.
  * **Hardware SDK:** Nothing Glyph Developer Kit (`com.nothing.kgu:glyph-integration`).
  * **Architecture Pattern:** MVVM with Clean Architecture principles.

-----

## 🚀 Getting Started

### Prerequisites

1.  **Hardware:** Nothing Phone (1), (2), or (2a). (The app runs on standard Android devices in "Mock Mode" but requires Glyph hardware for full functionality).
2.  **IDE:** Android Studio Ladybug or newer.
3.  **OS:** Nothing OS 2.5+ (Android 14).

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/glyph-glance.git
    ```
2.  **Open in Android Studio:** Sync Gradle files.
3.  **Build & Run:** Deploy to your Nothing Phone via USB debugging.
4.  **Grant Permissions:** On first launch, use the **Permission Wizard** to grant:
      * Notification Access (Required to read incoming messages).
      * Glyph Interface Permissions (Required to control lights).

-----

## 🧪 Testing & Simulation

Since waiting for real people to text you is inefficient for debugging, we include a Python-based **Notification Injection Tool**.

**Script:** `scripts/mock_notifier.py`

This tool uses ADB to inject mock notifications into the device, simulating various scenarios:

  * **Split Texter Burst:** Sends 5 messages in \<1 second to test the Buffer Engine.
  * **Urgent Alert:** Sends a message with keywords like "Deadline" to test the Red Strobe trigger.
  * **Casual Chat:** Sends a neutral message to test the standard processing path.

**Usage:**

```bash
python3 scripts/mock_notifier.py
```

-----

## 📂 Project Structure

```text
glyph-glance/
├── composeApp/
│   ├── src/androidMain/       # Android-specific code (Glyph SDK, Services)
│   │   ├── kotlin/.../service/   # NotificationListener & GlyphManager
│   │   └── AndroidManifest.xml   # Service declarations
│   ├── src/commonMain/        # Shared KMP Logic (AI, DB, UI)
│   │   ├── kotlin/.../ai/        # CactusManager & LLM Wrappers
│   │   ├── kotlin/.../database/  # Room Entities & DAOs
│   │   ├── kotlin/.../logic/     # Core Business Rules
│   │   └── kotlin/.../ui/        # Jetpack Compose Screens
├── docs/                      # Detailed Track Documentation & Specs
├── scripts/                   # Python Testing Tools
└── gradle/                    # Build configuration
```

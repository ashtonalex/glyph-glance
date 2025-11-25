# **Product Requirements Document (PRD): Glyph-Glance (Adaptive Edition)**

## **1\. Product Overview & Goals**

Product Name: Glyph-Glance (Adaptive Edition)

Platform: Android (Nothing OS 2.5+)

Device Target: Nothing Phone (2a) / Nothing Phone (2)

Core Problem:

Mobile users suffer from "Notification Fatigue," unable to distinguish between genuine emergencies, emotional nuance, and fragmented "split-text" spam without unlocking their device. Cloud-based solutions compromise privacy.

**Success Metrics (KPIs):**

1. **Phantom Buzz Reduction:** \>30% of incoming notifications are buffered or filtered (do not trigger a vibration/light) during "Focus Mode."  
2. **Latency:** Single message inference latency \< 600ms (from receipt to Glyph trigger).  
3. **Battery Impact:** Daily battery consumption \< 6% with background monitoring active.

---

## **2\. Target User & User Stories**

**Primary Persona:** "The Deep Worker." Tech-savvy, privacy-conscious, owns a Nothing Phone. They communicate with high-frequency "split texters" (friends) and low-frequency "serious" contacts (boss/family).

**User Stories:**

1. **As a user with a "split texter" friend,** I want the app to buffer multiple short texts received within 10 seconds into a single Glyph alert, so I am not disturbed by constant buzzing.  
2. **As a non-technical user,** I want to type "Only light up if Mark mentions dinner or an emergency" in plain English, so that the system automatically configures the filtering rules without me navigating complex menus.  
3. **As a user with anxiety,** I want to see a specific "Amber" breathing light if a message is detected as aggressive or negative, so I can mentally prepare before unlocking the phone.  
4. **As a privacy advocate,** I want to verify that my notification data is being processed locally by seeing the "Airplane Mode" indicator while the app still functions perfectly.  
5. **As a customized setup lover,** I want to assign the "Rapid Strobe" Glyph pattern specifically to the highest urgency level, so I never miss a critical alert.

---

## **3\. Technical Stack & Architecture**

### **Frontend (UI Layer)**

* **Language:** Kotlin.  
* **Framework:** Jetpack Compose (Material 3 Design).  
* **State Management:** Jetpack ViewModel \+ Kotlin StateFlow.  
* **Navigation:** Jetpack Navigation Compose.

### **Backend (Local Service Layer)**

* **Core Logic:** Kotlin Multiplatform (KMP) shared module (for potentially sharing logic later, though Android-heavy for MVP).  
* **Background Service:** Android NotificationListenerService (bound to a persistent foreground service to prevent OS killing).  
* **AI Inference Engine:** **Cactus SDK** (Android).  
* **Model:** Qwen3-0.6B-Instruct-GGUF (Running on CPU/NPU).  
* **Hardware Integration:** **Nothing Glyph Developer Kit** (v1.5+).

### **Database (Local Storage)**

* **Engine:** SQLite via **Room Database**.  
* **Schema Strategy:** Relational.  
* **Data Persistence:** Asynchronous writes via Kotlin Coroutines/Flow.

### **Permissions Required**

* BIND\_NOTIFICATION\_LISTENER\_SERVICE  
* POST\_NOTIFICATIONS  
* FOREGROUND\_SERVICE  
* com.nothing.kgu.permission.GLYPH\_INTERFACE (Specific to Nothing Phone).

---

## **4\. Core Feature Specifications**

### **Feature 1: Adaptive Split-Text Buffering Engine**

Functionality:

A state-machine based engine that intercepts notifications and decides whether to notify immediately or buffer based on sender behavior.

**Logic & Calculations:**

1. **Check ContactBehaviorTable**: Retrieve avg\_inter\_message\_time for the sender.  
2. **Classification**:  
   * IF avg\_inter\_message\_time \< 15 seconds AND message\_count\_last\_hour \> 5 $\\rightarrow$ Flag as **SPLIT\_TEXTER**.  
   * ELSE $\\rightarrow$ Flag as **NORMAL**.  
3. **State Machine**:  
   * **State: IDLE** $\\rightarrow$ Incoming Msg $\\rightarrow$ Transition to **BUFFERING** (start BufferTimer \= 20s).  
   * **State: BUFFERING** $\\rightarrow$ New Msg $\\rightarrow$ Reset BufferTimer; Append text to BufferQueue.  
   * **Trigger FLUSH**: Occurs if BufferTimer expires OR BufferQueue.size \> 5\.  
4. **Action on FLUSH**: Concatenate all strings in BufferQueue. Send concatenated string to **Feature 2 (Inference)**.

### **Feature 2: Natural Language Rule Configuration**

Functionality:

Allows users to input natural language rules which are converted into a structured JSON configuration for the filtering engine.

**Tool Usage (Cactus/Qwen3):**

* **Input:** User string (e.g., "Block everything unless it's from Mom about the hospital").  
* **System Prompt:**  
  "You are a JSON converter. Extract logic from the user input.  
  Return ONLY JSON. Schema:  
  {  
  'whitelist\_keywords': \[strings\],  
  'whitelist\_contacts': \[strings\],  
  'min\_urgency\_level': 'LOW'|'MEDIUM'|'HIGH'  
  }"  
* **Output Processing:** The app parses the JSON and inserts/updates the RulesTable in the Room Database.

---

## **5\. Database Schema & Data Structure**

**Table 1: ContactProfile**

SQL

CREATE TABLE ContactProfile (  
    contact\_id TEXT PRIMARY KEY, \-- Hash of phone number/sender ID  
    is\_split\_texter BOOLEAN DEFAULT 0,  
    baseline\_sentiment FLOAT, \-- 0.0 (Negative) to 1.0 (Positive)  
    last\_message\_timestamp LONG  
);

**Table 2: Rules**

SQL

CREATE TABLE Rules (  
    rule\_id INTEGER PRIMARY KEY AUTOINCREMENT,  
    raw\_instruction TEXT, \-- "Don't disturb me..."  
    json\_logic TEXT, \-- Stored JSON blob  
    is\_active BOOLEAN DEFAULT 1  
);

**Validation Rules:**

* json\_logic must be validated against the internal generic schema before insertion.  
* baseline\_sentiment must be strictly between 0.0 and 1.0.

---

## **6\. API Specifications (Internal Interfaces)**

Since this is a local app, these "APIs" represent the **Interface Contract** between the UI/ViewModel and the Background Repository/Cactus Engine.

### **Interface 1: POST /inference/analyze\_notification**

Purpose: Sends text (or buffered text batch) to the local LLM for classification.

Caller: NotificationListenerService

Method Signature: suspend fun analyzeText(content: String, sender: String): AnalysisResult

**Request Object (Internal Data Class):**

Kotlin

data class AnalysisRequest(  
    val content: String, // "Hey... are you there?... we need to talk"  
    val senderId: String,  
    val timestamp: Long  
)

**Response Object (Return Value):**

Kotlin

data class AnalysisResult(  
    val urgencyScore: Int, // 1 (Low) to 5 (Critical)  
    val sentiment: String, // "POSITIVE", "NEGATIVE", "NEUTRAL"  
    val triggeredRuleId: Int? // Null if no specific rule matched  
)

**Logic Flow:**

1. Cactus receives content.  
2. Runs Qwen3 inference.  
3. Parses output.  
4. Returns AnalysisResult.

### **Interface 2: POST /config/create\_rule**

Purpose: User creates a new filtering rule via natural language.

Caller: SettingsViewModel

Request: String (User Input)

Response: Boolean (Success/Fail)

**Error Handling:**

* If Qwen3 returns malformed JSON $\\rightarrow$ Throw InferenceException.  
* If DB write fails $\\rightarrow$ Throw StorageException.

---

## **7\. Data Pipelines & Logic Flow**

**The "Notification Lifecycle" Pipeline:**

1. **Ingestion:**  
   * OS triggers onNotificationPosted.  
   * App extracts tickerText, title, text.  
2. **Pre-Processing (The Buffer):**  
   * Check ContactProfile.  
   * If SplitTexter $\\rightarrow$ Add to BufferQueue (Memory). WAIT.  
   * If Timeout $\\rightarrow$ FLUSH.  
3. **Processing (The Brain):**  
   * Send text to **Cactus Engine**.  
   * Input: \[System: Classify Urgency/Sentiment\] User: {Text}.  
   * Output: {"urgency": 4, "sentiment": "NEGATIVE"}.  
4. **Decision & Output:**  
   * Check RulesTable. Does Urgency \>= Threshold?  
   * **YES:** Call GlyphManager.  
     * If Urgency \== 5 $\\rightarrow$ Trigger GLYPH\_STROBE\_RED.  
     * If Sentiment \== NEGATIVE $\\rightarrow$ Trigger GLYPH\_BREATHE\_AMBER.  
   * **NO:** Suppress notification vibration (if possible) or do nothing.

---

## **8\. UI/UX Requirements (Technical Implementation)**

**Component Needs:**

1. **LiveLogTerminal Component:** A stylized, matrix-style scrolling text view that shows real-time logs of the background service (e.g., "Buffer: 3 msgs held...", "Inference: High Urgency detected"). This builds trust.  
2. **GlyphPreviewButton:** A UI element that, when tapped, triggers the actual hardware Glyph light sequence on the back of the phone for previewing configurations.  
3. **RuleInputCard:** A text field with a "Magic Wand" icon. When clicked, it shows a loading spinner (Cactus processing) and then animates the "Extracted Rules" appearing below.

**Accessibility:**

* All Glyph patterns must have a **Haptic Feedback** equivalent (e.g., Rapid Strobe \= Rapid Vibration) for visually impaired users.  
* **TalkBack:** All log entries must be readable by screen readers.

---

## **9\. Crucial Constraints for the Coding Agent**

1. **Nothing Glyph SDK:** You must use the com.nothing.kgu:glyph-integration library. Do not mock this; assume the API exists. Use GlyphFrame.Builder to construct light patterns.  
2. **Service Lifecycle:** The NotificationListenerService is passive. You must implement a separate ForegroundService with a persistent notification ("Glyph-Glance is Active") to keep the process alive in memory on Android.  
3. **Cactus Integration:** Assume a singleton CactusEngine class that wraps the underlying C++ calls. Do not try to implement the raw C++ JNI; assume the SDK provides a Kotlin-friendly generate(prompt: String) suspend function.

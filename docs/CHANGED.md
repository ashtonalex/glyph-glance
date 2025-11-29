# LLM Response Handling Report

This document provides a comprehensive overview of how LLM (Large Language Model) responses are toggled, handled, and logged within the Glyph Glance application.

---

## Table of Contents

1. [Overview](#overview)
2. [Toggle Mechanisms](#toggle-mechanisms)
3. [Entry Points / Key Functions](#entry-points--key-functions)
4. [Response Handling Flow](#response-handling-flow)
5. [Logging Strategy](#logging-strategy)
6. [Code References](#code-references)

---

## Overview

Glyph Glance uses the **Cactus SDK** with the **Qwen3-0.6B** model to perform on-device sentiment analysis and urgency scoring of notifications. The LLM integration enables intelligent prioritization of notifications without requiring cloud connectivity.

The system has two primary toggle mechanisms:
- A **mock mode toggle** for development/testing without the actual model
- A **UI visibility toggle** for showing/hiding raw AI responses in the developer interface

---

## Toggle Mechanisms

### 1. Mock Mode Toggle (`USE_MOCK`)

**Location:** `composeApp/src/commonMain/kotlin/com/example/glyph_glance/ai/CactusManager.kt`

```kotlin
class CactusManager {
    // Toggle this to FALSE when testing on a real device with the model downloaded
    private val USE_MOCK = false // Changed to false as per user request
    // ...
}
```

**Behavior:**
- When `USE_MOCK = true`: Uses `mockAnalysis()` which returns hardcoded urgency/sentiment based on simple keyword matching
- When `USE_MOCK = false`: Downloads and initializes the Qwen3-0.6 model, then performs real LLM inference

**Impact on Functions:**

| Function | Mock Behavior | Real Behavior |
|----------|---------------|---------------|
| `initialize()` | Skips model download/initialization | Downloads model, initializes with 2048 context size |
| `analyzeText()` | Returns mock urgency (1 or 5) and sentiment | Sends prompt to LLM, parses JSON response |
| `translateRule()` | Returns empty JSON `"{}"` | Sends natural language to LLM for JSON schema conversion |

### 2. UI Visibility Toggle (`showRawAiResponse`)

**Location:** `composeApp/src/commonMain/kotlin/com/example/glyph_glance/DashboardScreen.kt`

```kotlin
// Sentiment Analysis Test state
var showRawAiResponse by remember { mutableStateOf(false) }
```

**Behavior:**
- Controls visibility of the raw AI response text in the "AI Sentiment Test" section
- Only visible when Developer Mode is enabled
- Toggled via clickable row with expand/collapse animation

**UI Implementation:**

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onShowRawAiResponseChange(!showRawAiResponse) },
    // ...
) {
    Text(text = "Raw AI Response:")
    Icon(
        imageVector = if (showRawAiResponse) Icons.Default.ArrowDropUp 
                      else Icons.Default.ArrowDropDown,
        // ...
    )
}

AnimatedVisibility(
    visible = showRawAiResponse,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
) {
    // Display raw response in monospace font
    Text(
        text = rawResponse,
        fontFamily = FontFamily.Monospace
    )
}
```

---

## Entry Points / Key Functions

### 1. `SentimentAnalysisService.analyzeNotification()`

**Location:** `composeApp/src/commonMain/kotlin/com/example/glyph_glance/logic/SentimentAnalysisService.kt`

**Purpose:** Primary service for analyzing notification content using the Qwen3 LLM.

**Signature:**
```kotlin
suspend fun analyzeNotification(
    content: String,
    senderId: String
): AnalysisResult?
```

**Key Operations:**
1. Initializes CactusLM if not already initialized
2. Infers app context from sender ID (work, messaging, gaming, etc.)
3. Constructs system prompt for urgency evaluation (1-6 scale)
4. Calls `lm.generateCompletion()` with zero temperature for deterministic output
5. Parses urgency score from response using multiple fallback strategies

**Return Type:**
```kotlin
data class AnalysisResult(
    val urgencyScore: Int,        // 1-6 scale
    val sentiment: String,        // "POSITIVE", "NEGATIVE", "NEUTRAL"
    val triggeredRuleId: Int?,    // Rule ID if matched
    val rawAiResponse: String?    // Raw LLM output for debugging
)
```

---

### 2. `CactusManager.analyzeText()`

**Location:** `composeApp/src/commonMain/kotlin/com/example/glyph_glance/ai/CactusManager.kt`

**Purpose:** Legacy/alternative interface for text analysis with JSON response parsing.

**Signature:**
```kotlin
suspend fun analyzeText(text: String, historyContext: String?): AIResult
```

**Key Operations:**
1. Checks `USE_MOCK` flag for mock vs real analysis
2. Constructs prompt requesting JSON format: `{"urgencyScore": <int>, "sentiment": <string>}`
3. Parses response with basic regex/string matching

**Return Type:**
```kotlin
data class AIResult(val urgencyScore: Int, val sentiment: String)
```

---

### 3. `GlyphIntelligenceEngine.processNotification()`

**Location:** `composeApp/src/commonMain/kotlin/com/example/glyph_glance/logic/GlyphIntelligenceEngine.kt`

**Purpose:** Orchestrates the full notification processing pipeline.

**Signature:**
```kotlin
override suspend fun processNotification(text: String, senderId: String): DecisionResult
```

**Key Operations:**
1. Fetches or creates contact profile
2. Updates contact statistics
3. Calls `cactusManager.analyzeText()` for AI analysis
4. Fetches active rules from database
5. Matches rules against AI result and notification content
6. Returns decision (should light up, glyph pattern)

**Return Type:**
```kotlin
data class DecisionResult(
    val shouldLightUp: Boolean,
    val pattern: GlyphPattern
)
```

---

### 4. `GlyphNotificationListenerService.onNotificationPosted()`

**Location:** `composeApp/src/androidMain/kotlin/com/example/glyph_glance/service/GlyphNotificationListenerService.kt`

**Purpose:** Android system service that intercepts all device notifications.

**Signature:**
```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification)
```

**Key Operations:**
1. Extracts notification title and message
2. Determines base priority from Android notification importance
3. Performs sentiment analysis via `sentimentAnalysisService.analyzeNotification()`
4. Loads keyword and app rules from preferences
5. Calculates final priority using `calculatePriority()`
6. Combines AI score with rule-based score (takes maximum)
7. Saves notification to Room database with all metadata

---

### 5. `calculatePriority()`

**Location:** `composeApp/src/commonMain/kotlin/com/example/glyph_glance/logic/PriorityCalculator.kt`

**Purpose:** Combines AI urgency with user-defined rules to determine final priority.

**Signature:**
```kotlin
fun calculatePriority(
    text: String,
    appPackage: String,
    appName: String? = null,
    keywordRules: List<KeywordRule>,
    appRules: List<AppRule>,
    basePriority: NotificationPriority
): NotificationPriority
```

**Priority Logic:**
- HIGH priority rules override all other priorities
- MEDIUM priority can only be overridden by HIGH
- Checks both keyword rules and app-specific rules
- Returns highest matched priority or base priority if no rules match

---

## Response Handling Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NOTIFICATION RECEIVED                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  GlyphNotificationListenerService.onNotificationPosted()                    │
│  - Extract title, message, app name                                         │
│  - Determine base priority from Android importance                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  SentimentAnalysisService.analyzeNotification()                             │
│  - Infer app context from sender ID                                         │
│  - Build system + user prompt for Qwen3                                     │
│  - Call CactusLM.generateCompletion()                                       │
│  - Parse urgency score (1-6) from response                                  │
│  - Return AnalysisResult with rawAiResponse                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  calculatePriority()                                                        │
│  - Load keyword rules and app rules                                         │
│  - Match against notification text                                          │
│  - Return highest priority found                                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  FINAL URGENCY CALCULATION                                                  │
│  - If rules matched: max(aiUrgencyScore, ruleBasedUrgencyScore)             │
│  - If no rules: use AI urgency score directly                               │
│  - Convert to NotificationPriority enum                                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  NotificationRepository.insertNotification()                                │
│  - Save to Room database with:                                              │
│    • title, message, timestamp                                              │
│    • appPackage, appName                                                    │
│    • priority, sentiment, urgencyScore                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Logging Strategy

### Console Logging (`println`)

**Location:** `SentimentAnalysisService.kt`

All console logs use the prefix `SentimentAnalysisService:` for easy filtering.

| Stage | Log Message |
|-------|-------------|
| Model Download | `"Downloading Qwen3-0.6 model..."` |
| Download Result | `"Download result: $downloadResult"` |
| Model Init | `"Initializing Qwen3-0.6 model..."` |
| Init Result | `"Initialization result: $initResult"` |
| Init Success | `"Qwen3 model initialized successfully"` |
| Init Error | `"Error initializing Cactus LM: ${e.message}"` |
| Prompt Sent | `"Sending prompt to Qwen3 for message: '$content'"` |
| Response Received | `"Qwen3 response - Success: ${result?.success}, Response: '$responseText'"` |
| Analysis Failed | `"Failed to get analysis result from Cactus"` |
| Parsing | `"Parsing urgency score from response: '$trimmedResponse'"` |
| Parse Warning | `"WARNING - No number found in response: '$trimmedResponse'"` |
| Default Score | `"Using default score: 3"` |
| Parse Error | `"Error parsing urgency score: ${e.message}"` |

### Android Logging (`Log.d/e/w`)

**Location:** `GlyphNotificationListenerService.kt`

Uses tag `GlyphNotificationListener` for logcat filtering.

| Level | Usage |
|-------|-------|
| `Log.d` | Service lifecycle, successful operations, saved notifications |
| `Log.w` | Failed initialization warnings |
| `Log.e` | Errors during processing, save failures |

**Example Log Output:**
```
D/GlyphNotificationListener: Notification Listener Service Created
D/GlyphNotificationListener: Sentiment analysis service initialized successfully
D/GlyphNotificationListener: Saved notification: Meeting Reminder from Calendar 
    (Priority: HIGH (rule-based: MEDIUM, base: LOW), 
     Sentiment: NEUTRAL, AI Urgency: 4, Final Urgency: 5)
```

---

## Code References

### Key Files

| File | Purpose |
|------|---------|
| `ai/CactusManager.kt` | Mock toggle, legacy LLM interface |
| `logic/SentimentAnalysisService.kt` | Primary LLM analysis service |
| `logic/GlyphIntelligenceEngine.kt` | Notification processing orchestration |
| `logic/PriorityCalculator.kt` | Rule-based priority calculation |
| `service/GlyphNotificationListenerService.kt` | Android notification interception |
| `DashboardScreen.kt` | UI toggle for raw AI response visibility |

### Cactus SDK Integration

The app uses the Cactus SDK (v1.2.0-beta) for on-device LLM inference:

```kotlin
// Initialization
val lm = CactusLM()
lm.downloadModel("qwen3-0.6")
lm.initializeModel(CactusInitParams(model = "qwen3-0.6", contextSize = 2048))

// Inference
val result = lm.generateCompletion(
    messages = listOf(
        ChatMessage(role = "system", content = systemPrompt),
        ChatMessage(role = "user", content = userPrompt)
    ),
    params = CactusCompletionParams(
        maxTokens = 500,
        temperature = 0.0  // Deterministic output
    )
)
```

### Response Parsing Strategy

The `parseUrgencyScore()` function uses multiple fallback strategies:

1. **Strategy 1:** Check if response starts with number 1-6 (most common)
2. **Strategy 2:** Find all numbers 1-6 in response, take the last one
3. **Strategy 3:** Find any digit, clamp to 1-6 range
4. **Strategy 4:** Default to score 3 (medium urgency)

This ensures robust parsing even when the LLM includes explanation text despite instructions.

---

## Summary

The Glyph Glance LLM integration provides:

- **Flexible toggling** via mock mode for development and UI toggle for debugging
- **Comprehensive logging** at both console and Android logcat levels
- **Robust response handling** with multiple parsing fallback strategies
- **Rule augmentation** where user-defined rules can boost (but never lower) AI scores
- **Full traceability** with raw AI responses stored in `AnalysisResult.rawAiResponse`


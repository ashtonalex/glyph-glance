Here is the comprehensive developer guide for **Track 1: The Brain**. This file is designed to be handed directly to the team member responsible for the Core AI, Database, and Business Logic.

---

# Track 1: The Brain (Core AI & Data Logic)

**Owner:** [Developer Name]
**Focus:** Pure Logic, Data Persistence, and Cactus/AI Integration.
**Dependencies:** None (This track can be built in isolation using Mocks).

---

## 1. Overview

You are building the "intelligence" of Glyph-Glance. Your code is responsible for receiving raw text, understanding its context (using local AI), checking it against user-defined rules, and outputting a final decision (e.g., "Flash Red"). You also own the database that remembers who is a "split-texter" and what the user's natural language rules are.

## 2. Technical Stack & Dependencies

Add these to your module-level `build.gradle.kts`:

  * **Local DB:** `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler` (KSP).
  * **AI Engine:** `io.cactus:cactus-android` (or equivalent SDK artifact).
  * **Serialization:** `kotlinx.serialization`.
  * **Coroutines:** `kotlinx.coroutines.core`.

---

## 3. Data Layer (Room Database)

You need to implement the persistent storage for contact profiles and user rules.

### A. Entities

**1. `ContactProfile.kt`**
This tracks the behavior of people texting the user.

```kotlin
@Entity(tableName = "contact_profiles")
data class ContactProfile(
    @PrimaryKey val senderId: String, // Hash or raw phone number
    val isSplitTexter: Boolean = false,
    val avgInterMessageTimeMillis: Long = 0L, // Running average
    val baselineSentiment: Float = 0.5f, // 0.0 (Neg) -> 1.0 (Pos)
    val lastMessageTimestamp: Long = 0L
)
```

**2. `Rule.kt`**
This stores the filtering rules created by the user (or the AI).

```kotlin
@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawInstruction: String, // "Don't disturb unless it's Mom"
    val jsonLogic: String, // Serialized JSON: {"whitelist": ["Mom"], "min_urgency": 4}
    val isActive: Boolean = true
)
```

### B. DAOs (Data Access Objects)

Create `ContactDao` and `RuleDao` with standard CRUD operations.

  * **Critical Query:** `findActiveRules()`: Returns all rules where `isActive = true`.
  * **Critical Query:** `getProfile(senderId)`: Returns the profile to check if they are a split-texter.

---

## 4. AI Layer (Cactus Integration)

You are responsible for wrapping the `Cactus` SDK. Since the model (Qwen3) might not be ready on Day 1, you must implement a **Mock Mode**.

### A. The Wrapper Class (`CactusManager`)

```kotlin
class CactusManager(context: Context) {
    
    // Toggle this to FALSE when testing on a real device with the model downloaded
    private val USE_MOCK = true 

    suspend fun analyzeText(text: String, historyContext: String?): AIResult {
        if (USE_MOCK) return mockAnalysis(text)
        
        // TODO: Real Implementation
        // 1. Construct Prompt: "Analyze urgency (1-5) and sentiment: $text"
        // 2. Call Cactus.generate(prompt)
        // 3. Parse JSON output
        return realAnalysis(text)
    }

    private fun mockAnalysis(text: String): AIResult {
        // Simple logic for testing without model
        val urgency = if (text.contains("urgent", ignoreCase = true)) 5 else 1
        val sentiment = if (text.contains("happy", ignoreCase = true)) "POSITIVE" else "NEUTRAL"
        return AIResult(urgency, sentiment)
    }
}

data class AIResult(val urgencyScore: Int, val sentiment: String)
```

### B. Natural Language Processor

Implement a function that translates user input into the JSON rule format expected by the DB.

  * **Function:** `translateRule(userInput: String): String (JSON)`
  * **Prompt Strategy:** "You are a config parser. Convert '$userInput' into valid JSON schema..."

---

## 5. Logic Layer (The Interface Implementation)

You must implement the contracts agreed upon with Track 2 (System) and Track 3 (UI).

### A. `IntelligenceEngine` Implementation

This is the main entry point called by the Background Service (Track 2).

```kotlin
class GlyphIntelligenceEngine(
    private val cactusManager: CactusManager,
    private val ruleDao: RuleDao,
    private val contactDao: ContactDao
) : IntelligenceEngine {

    override suspend fun processNotification(text: String, senderId: String): DecisionResult {
        // 1. Fetch Profile & Check "Split Texter" Logic
        val profile = contactDao.getProfile(senderId)
        // (Note: Track 2 handles the buffering, but you handle the profile update)
        updateContactStats(profile, senderId)

        // 2. AI Analysis
        val aiResult = cactusManager.analyzeText(text, null)

        // 3. Rule Matching
        val activeRules = ruleDao.getAllRules()
        val triggeredRule = matchRules(aiResult, text, senderId, activeRules)

        // 4. Determine Output
        return if (triggeredRule != null || aiResult.urgencyScore >= 4) {
             DecisionResult(shouldLightUp = true, pattern = GlyphPattern.URGENT)
        } else {
             DecisionResult(shouldLightUp = false, pattern = GlyphPattern.NONE)
        }
    }

    private fun matchRules(ai: AIResult, text: String, sender: String, rules: List<Rule>): Rule? {
        // Iterate through JSON rules and see if conditions are met
        // Return the first matching rule or null
    }
}
```

### B. `RulesRepository` Implementation

This is called by the UI (Track 3) when the user types a new setting.

```kotlin
class AppRulesRepository(
    private val cactusManager: CactusManager, 
    private val ruleDao: RuleDao
) {
    suspend fun addNaturalLanguageRule(userText: String) {
        // 1. Ask AI to convert text -> JSON
        val json = cactusManager.translateRule(userText)
        // 2. Save to DB
        ruleDao.insert(Rule(rawInstruction = userText, jsonLogic = json))
    }
    
    fun getRulesFlow() = ruleDao.getAllRulesFlow()
}
```

---

## 6. Development Checklist (Day 1-2)

1.  [ ] **Setup Room DB:** Create the abstract `AppDatabase` class and connect it.
2.  [ ] **Create Data Models:** Copy/Paste the Entity data classes above.
3.  [ ] **Implement Mock AI:** Get `CactusManager` returning fake data immediately so Track 2 can test their service.
4.  [ ] **Rule Logic:** Write the unit tests for `matchRules()`. Ensure that if a rule says "whitelist: Mom", a text from "Mom" returns TRUE.
5.  [ ] **DI Setup:** Create a simple Singleton/Hilt module to provide instances of `IntelligenceEngine` to the rest of the app.

## 7. Interfaces to Expose (Copy to `Shared` Module)

Ensure these interfaces are visible to the other developers:

```kotlin
// Consumed by Track 2 (Background Service)
interface IntelligenceEngine {
    suspend fun processNotification(text: String, sender: String): DecisionResult
}

// Consumed by Track 3 (UI)
interface RulesRepository {
    suspend fun addNaturalLanguageRule(text: String)
    fun getRulesFlow(): Flow<List<Rule>>
}

data class DecisionResult(
    val shouldLightUp: Boolean,
    val pattern: GlyphPattern // Enum: RED_STROBE, AMBER_BREATHE, NONE
)
```
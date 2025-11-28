Here is the comprehensive reference documentation for the Cactus Kotlin SDK, formatted as a pure Markdown file for Antigravity to use.

-----

# Cactus Kotlin SDK Reference Guide

**Version:** 1.2.0-beta
**Target:** Kotlin Multiplatform (Android & iOS)
**Documentation Source:** [cactuscompute.com/docs/kotlin](https://cactuscompute.com/docs/kotlin)

-----

## 1\. Installation & Setup

### Dependency Resolution

Add to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

### Module Dependencies

Add to your KMP project's `build.gradle.kts` in `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.cactuscompute:cactus:1.2.0-beta")
            }
        }
    }
}
```

### Android Manifest Permissions

Required for downloading models and audio recording (if using STT).

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

-----

## 2\. Initialization

**Crucial Step:** You must initialize the Cactus context in your Android Activity before using any SDK features.

```kotlin
import com.cactus.CactusContextInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Context
        CactusContextInitializer.initialize(this)
        
        // ... rest of app logic
    }
}
```

-----

## 3\. Language Model (CactusLM)

The core class for Text Generation, Chat, Function Calling, and Embeddings.

### Core Methods

| Method | Description | Signature |
| :--- | :--- | :--- |
| `downloadModel` | Downloads a model by slug. Defaults to "qwen3-0.6". | `suspend fun downloadModel(model: String = "qwen3-0.6"): Boolean` |
| `initializeModel` | Loads model into memory. | `suspend fun initializeModel(params: CactusInitParams): Boolean` |
| `generateCompletion` | Runs inference. Supports streaming via callback. | `suspend fun generateCompletion(messages: List<ChatMessage>, params: CactusCompletionParams, onToken: Callback?): CactusCompletionResult?` |
| `generateEmbedding` | Creates vector embeddings. | `suspend fun generateEmbedding(text: String, modelName: String?): CactusEmbeddingResult?` |
| `getModels` | Lists available models. | `suspend fun getModels(): List<CactusModel>` |
| `unload` | Frees memory. | `fun unload()` |

### Usage Examples

#### Basic Text Completion

```kotlin
val lm = CactusLM()
try {
    // 1. Download & Init
    lm.downloadModel("qwen3-0.6")
    lm.initializeModel(CactusInitParams(model = "qwen3-0.6", contextSize = 2048))

    // 2. Generate
    val result = lm.generateCompletion(
        messages = listOf(ChatMessage(content = "Hello!", role = "user"))
    )

    if (result?.success == true) {
        println("Response: ${result.response}")
    }
} finally {
    lm.unload()
}
```

#### Streaming Responses

```kotlin
val result = lm.generateCompletion(
    messages = listOf(ChatMessage("Write a poem", "user")),
    params = CactusCompletionParams(maxTokens = 200),
    onToken = { token, tokenId ->
        print(token) // Updates in real-time
    }
)
```

#### Function Calling (Tools)

```kotlin
import com.cactus.models.createTool
import com.cactus.models.ToolParameter

val tools = listOf(
    createTool(
        name = "get_weather",
        description = "Get weather for location",
        parameters = mapOf(
            "location" to ToolParameter(type = "string", description = "City", required = true)
        )
    )
)

val result = lm.generateCompletion(
    messages = listOf(ChatMessage("Weather in London?", "user")),
    params = CactusCompletionParams(tools = tools)
)
// Result will contain tool calls if triggered
```

#### Vision (Multimodal)

```kotlin
val visionModel = lm.getModels().first { it.supports_vision }
lm.downloadModel(visionModel.slug)
lm.initializeModel(CactusInitParams(model = visionModel.slug))

val result = lm.generateCompletion(
    messages = listOf(
        ChatMessage(
            role = "user",
            content = "Describe this image",
            images = listOf("/path/to/local/image.jpg")
        )
    )
)
```

### Configuration & Data Classes

**`CactusInitParams`**

  * `model`: String? (Model slug)
  * `contextSize`: Int? (e.g., 2048)

**`CactusCompletionParams`**

  * `maxTokens`: Int (default 200)
  * `temperature`: Double
  * `topK`: Int
  * `topP`: Double
  * `stopSequences`: List\<String\>
  * `tools`: List\<Tool\>?
  * `mode`: InferenceMode (LOCAL, REMOTE, LOCAL\_FIRST, REMOTE\_FIRST)

**`ChatMessage`**

  * `role`: String ("user", "system", "assistant")
  * `content`: String
  * `images`: List\<String\>? (File paths)

-----

## 4\. Tool Filtering Service

Optimizes token usage by selecting only relevant tools before inference.

**Configuration:**

```kotlin
import com.cactus.services.ToolFilterConfig
import com.cactus.services.ToolFilterStrategy

val lm = CactusLM(
    enableToolFiltering = true,
    toolFilterConfig = ToolFilterConfig(
        strategy = ToolFilterStrategy.SEMANTIC, // or SIMPLE
        maxTools = 3,
        similarityThreshold = 0.5
    )
)
```

-----

## 5\. Speech-to-Text (CactusSTT)

Provides local transcription using Whisper models.

### Core Methods

| Method | Description |
| :--- | :--- |
| `downloadModel` | Downloads "whisper-tiny" or "whisper-base". |
| `initializeModel` | Loads the STT model. |
| `transcribe` | Transcribes audio file to text. |
| `isModelDownloaded` | Checks model status. |

### Usage Example

```kotlin
val stt = CactusSTT()

// Setup
stt.downloadModel("whisper-tiny")
stt.initializeModel(CactusInitParams(model = "whisper-tiny"))

// Execute
val result = stt.transcribe(
    filePath = "/path/to/audio.wav",
    params = CactusTranscriptionParams()
)

if (result?.success == true) {
    println("Text: ${result.text}")
    println("Time: ${result.processingTime}ms")
}
```

-----

## 6\. Performance Best Practices

1.  **Model Selection:** Prefer quantized, smaller models (e.g., Qwen3-0.6B, Gemma-270M) for mobile devices.
2.  **Context Management:** Keep `contextSize` as low as required (e.g., 2048) to reduce RAM usage.
3.  **Lifecycle:** Always call `unload()` in `onDestroy` or when the feature is done to prevent memory leaks.
4.  **Discovery:** Use `getModels()` to find supported models; results are cached locally.
5.  **Telemetry:** Optional monitoring available via `CactusTelemetry.setTelemetryToken("TOKEN")`.
Here is the comprehensive developer guide for **Track 3: The Face**. This file is designed to be handed directly to the team member responsible for UI/UX, User Configuration, and Visualization.

-----

# Track 3: The Face (UI/UX & User Configuration)

**Owner:** [Developer Name]
**Focus:** Jetpack Compose, State Management (MVVM), Permissions, and "Nothing OS" Aesthetic.
**Dependencies:** Observes `LiveLogger` (Track 2); Calls `RulesRepository` (Track 1).

-----

## 1\. Overview

You are building the interface that makes the user trust the AI. Since the "Body" works invisibly in the background, your UI is the only proof that the app is working. You need to build a "Matrix-style" live terminal to show the AI's thoughts, a simple input for natural language rules, and a robust onboarding flow to handle the complex permissions required.

## 2\. Technical Stack & Dependencies

Add these to your module-level `build.gradle.kts`:

  * **UI Framework:** `androidx.compose.ui:ui`, `androidx.compose.material3:material3`.
  * **Navigation:** `androidx.navigation:navigation-compose`.
  * **Fonts:** Import a **Dot Matrix** font (e.g., "NDot" or similar) to match Nothing OS.
  * **Icons:** `androidx.compose.material:material-icons-extended`.
  * **Lifecycle:** `androidx.lifecycle:lifecycle-viewmodel-compose`.

-----

## 3\. Core Component A: The Live Terminal (Trust UI)

This is the home screen. It visualizes the background logs from Track 2 so the user feels "in the loop."

### `LiveLogViewModel.kt`

This ViewModel connects the UI to the global `LiveLogger` object.

```kotlin
class LiveLogViewModel : ViewModel() {
    // Observe the singleton Logger from Track 2
    val logs: StateFlow<List<String>> = LiveLogger.logs
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
```

### `TerminalScreen.kt` (Jetpack Compose)

**Design:** Black background, Monospaced Red/White text, auto-scroll to bottom.

```kotlin
@Composable
fun TerminalScreen(viewModel: LiveLogViewModel = viewModel()) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll effect
    LaunchedEffect(logs.size) {
        listState.animateScrollToItem(logs.size)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)
    ) {
        Text(
            text = "GLYPH_GLANCE_DAEMON_V1.0",
            fontFamily = DotMatrixFont, // Custom Font
            color = Color.Red,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(state = listState) {
            items(logs) { log ->
                Text(
                    text = "> $log",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Divider(color = Color.DarkGray, thickness = 0.5.dp)
            }
        }
    }
}
```

-----

## 4\. Core Component B: Natural Language Rules (Config UI)

This allows users to type "Don't bother me" and see it convert to JSON.

### `RulesViewModel.kt`

Connects to Track 1's `RulesRepository`.

```kotlin
class RulesViewModel(private val repository: RulesRepository) : ViewModel() {
    
    var userInput by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    
    // Observe existing rules from Database
    val activeRules = repository.getRulesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun submitRule() {
        if (userInput.isBlank()) return
        
        viewModelScope.launch {
            isLoading = true
            try {
                // Call Track 1 to process AI Logic
                repository.addNaturalLanguageRule(userInput) 
                userInput = "" // Clear input
            } catch (e: Exception) {
                // Show Error Toast
            } finally {
                isLoading = false
            }
        }
    }
}
```

### `RuleConfigScreen.kt`

**Design:** Minimalist input field with a "Magic Wand" icon. Below it, a list of "Active Cards."

```kotlin
@Composable
fun RuleConfigScreen(viewModel: RulesViewModel = viewModel()) {
    val rules by viewModel.activeRules.collectAsState()
    
    Column(Modifier.padding(16.dp)) {
        // Input Area
        OutlinedTextField(
            value = viewModel.userInput,
            onValueChange = { viewModel.userInput = it },
            label = { Text("Type a rule (e.g. 'Only emergency')") },
            trailingIcon = {
                if (viewModel.isLoading) CircularProgressIndicator()
                else IconButton(onClick = { viewModel.submitRule() }) {
                    Icon(Icons.Default.AutoFixHigh, "Magic")
                }
            }
        )
        
        Spacer(Modifier.height(20.dp))
        
        // Rules List
        LazyColumn {
            items(rules) { rule ->
                RuleCard(rule)
            }
        }
    }
}

@Composable
fun RuleCard(rule: Rule) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
        Column(Modifier.padding(12.dp)) {
            Text(rule.rawInstruction, color = Color.White, fontWeight = FontWeight.Bold)
            Text(rule.jsonLogic, color = Color.Gray, fontSize = 10.sp, lineHeight = 12.sp)
        }
    }
}
```

-----

## 5\. Core Component C: Onboarding & Permissions

You cannot use the app without specific, hard-to-get permissions. You need a dedicated "Wizard."

### `PermissionWizard.kt`

**Logic:** Check for `NotificationListener` access. If missing, show a button that deep-links to the system settings.

```kotlin
fun Context.isNotificationServiceEnabled(): Boolean {
    val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
    return flat?.contains(packageName) == true
}

@Composable
fun PermissionWizard(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Required Access", fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))
        
        Button(onClick = {
            // DEEP LINK to special settings page
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            context.startActivity(intent)
        }) {
            Text("Grant Notification Access")
        }
        
        // Poll for permission change or add a "Done" button to re-check
    }
}
```

-----

## 6\. Integration Layer (The Glue)

### Connecting to Track 2 (Logs)

You need to observe the singleton created by Track 2.

  * **Object:** `LiveLogger`.
  * **Usage:** In your `LiveLogViewModel`, consume the flow: `LiveLogger.logs`.

### Connecting to Track 1 (Rules)

You need to call the Repository to save data.

  * **Interface:** `RulesRepository`.
  * **Usage:** Inject a Mock version of this repository first (that just adds the string to a list after 2 seconds) so you can build the UI before Track 1 finishes the AI logic.

-----

## 7\. Development Checklist (Day 1-2)

1.  [ ] **Theme Setup:** Configure `Theme.kt` to use a Dark Mode default and the Nothing-style Dot Matrix font (find a free `.ttf` and put in `res/font`).
2.  [ ] **Navigation Graph:** Set up `NavHost` with routes: `"onboarding"`, `"terminal"`, `"rules"`.
3.  [ ] **Mock Repository:** Create a class `MockRulesRepo` that implements `RulesRepository` so you can test the UI without the database.
4.  [ ] **Terminal UI:** Build the scrolling text view. Test it by adding a `LaunchedEffect` that pushes a dummy log every 500ms.
5.  [ ] **Permission Intent:** Verify that clicking the "Grant Access" button actually opens the correct Android System Settings page on your device.

-----

## 8\. Interfaces to Consume (Copy from Track 1/2)

Expect these to exist. If they don't, create dummy versions:

```kotlin
// From Track 1
interface RulesRepository {
    suspend fun addNaturalLanguageRule(text: String)
    fun getRulesFlow(): Flow<List<Rule>>
}

// From Track 1 Data Model
data class Rule(val id: Int, val rawInstruction: String, val jsonLogic: String)

// From Track 2 Shared Object
object LiveLogger {
    val logs = MutableStateFlow<List<String>>(emptyList())
    // You only read this, Track 2 writes to it
}
```
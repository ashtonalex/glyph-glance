package com.example.glyph_glance

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.glyph_glance.database.Rule
import com.example.glyph_glance.logic.RulesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Natural language rule configuration screen.
 * Users can type rules in plain English and the AI converts them to actionable filters.
 */
@Composable
fun RulesScreen(
    rulesRepository: RulesRepository? = null
) {
    val coroutineScope = rememberCoroutineScope()
    
    var userInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Collect rules from repository
    val rulesFlow: Flow<List<Rule>> = rulesRepository?.getRulesFlow() ?: flowOf(emptyList())
    val rules by rulesFlow.collectAsState(initial = emptyList())
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "AI Rules",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Create rules using natural language",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGrey,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Input Card with Magic Wand
        GlyphCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Example prompts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Examples: \"Only urgent messages\" or \"Ignore social media\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGrey
                    )
                }
                
                // Input field with magic wand button
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Type a rule (e.g. 'Alert me only for work emails')",
                            color = TextGrey.copy(alpha = 0.6f)
                        ) 
                    },
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonPurple,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    if (userInput.isNotBlank() && rulesRepository != null) {
                                        isLoading = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            try {
                                                rulesRepository.addNaturalLanguageRule(userInput)
                                                userInput = ""
                                            } catch (e: Exception) {
                                                errorMessage = "Failed to create rule: ${e.message}"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                enabled = userInput.isNotBlank() && rulesRepository != null
                            ) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = "Create Rule",
                                    tint = if (userInput.isNotBlank()) NeonPurple else TextGrey
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = NeonPurple
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = NeonRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                // Helper text when no repository
                if (rulesRepository == null) {
                    Text(
                        text = "Rules engine not available",
                        color = Color(0xFFFFAA00),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Active Rules Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Rules",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            
            // Rules count badge
            Box(
                modifier = Modifier
                    .background(NeonPurple.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${rules.size}",
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Rules list
        if (rules.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(SurfaceDarkGrey.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = TextGrey,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No rules yet",
                        color = TextGrey,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Type a rule above to get started",
                        color = TextGrey.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(rule = rule)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: Rule) {
    GlyphCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Rule instruction (user's original text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.rawInstruction,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Status badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (rule.isActive) NeonGreen else TextGrey,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (rule.isActive) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (rule.isActive) NeonGreen else TextGrey
                        )
                    }
                }
                
                // Delete button (placeholder - would need DAO method)
                IconButton(
                    onClick = { /* TODO: Delete rule */ },
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeonRed.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Rule",
                        tint = NeonRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // JSON Logic (parsed config)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Parsed Logic:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rule.jsonLogic.ifBlank { "{}" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextGrey,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}


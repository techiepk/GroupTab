package com.pennywiseai.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pennywiseai.tracker.ui.components.PennyWiseCard
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.components.PennyWiseTopBarWithBack
import com.pennywiseai.tracker.ui.theme.Spacing
import com.pennywiseai.tracker.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    
    PennyWiseScaffold(
        modifier = modifier,
        topBar = {
            PennyWiseTopBarWithBack(
                title = "Settings",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = Spacing.sm)
        ) {
        // Theme Settings Section
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
        )
        
        PennyWiseCard {
            // Theme Mode Selection
            Column {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = themeUiState.isDarkTheme == null,
                        onClick = { themeViewModel.updateDarkTheme(null) },
                        label = { Text("System") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeUiState.isDarkTheme == false,
                        onClick = { themeViewModel.updateDarkTheme(false) },
                        label = { Text("Light") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeUiState.isDarkTheme == true,
                        onClick = { themeViewModel.updateDarkTheme(true) },
                        label = { Text("Dark") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            
            // Dynamic Color Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Colors",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Use colors from your wallpaper",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = themeUiState.isDynamicColorEnabled,
                    onCheckedChange = { themeViewModel.updateDynamicColor(it) }
                )
            }
        }
        
            // More settings sections can be added here
        }
    }
}
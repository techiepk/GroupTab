package com.pennywiseai.tracker.ui.screens.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pennywiseai.tracker.domain.model.rule.*
import com.pennywiseai.tracker.ui.components.PennyWiseScaffold
import com.pennywiseai.tracker.ui.theme.Dimensions
import com.pennywiseai.tracker.ui.theme.Spacing
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    onNavigateBack: () -> Unit,
    onSaveRule: (TransactionRule) -> Unit,
    existingRule: TransactionRule? = null
) {
    var ruleName by remember { mutableStateOf(existingRule?.name ?: "") }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }

    // Simple condition state
    var selectedField by remember { mutableStateOf(TransactionField.AMOUNT) }
    var fieldDropdownExpanded by remember { mutableStateOf(false) }
    var selectedOperator by remember { mutableStateOf(ConditionOperator.LESS_THAN) }
    var conditionValue by remember { mutableStateOf("") }

    // Simple action state
    var actionField by remember { mutableStateOf(TransactionField.CATEGORY) }
    var actionValue by remember { mutableStateOf("") }

    // Common presets for quick setup
    val commonPresets = listOf(
        "Small amounts → Food" to {
            ruleName = "Small Food Payments"
            selectedField = TransactionField.AMOUNT
            selectedOperator = ConditionOperator.LESS_THAN
            conditionValue = "200"
            actionField = TransactionField.CATEGORY
            actionValue = "Food & Dining"
        },
        "Contains keyword → Category" to {
            ruleName = "Custom Category Rule"
            selectedField = TransactionField.SMS_TEXT
            selectedOperator = ConditionOperator.CONTAINS
            conditionValue = ""
            actionField = TransactionField.CATEGORY
            actionValue = ""
        },
        "Merchant name → Category" to {
            ruleName = "Merchant Category Rule"
            selectedField = TransactionField.MERCHANT
            selectedOperator = ConditionOperator.CONTAINS
            conditionValue = ""
            actionField = TransactionField.CATEGORY
            actionValue = ""
        }
    )

    PennyWiseScaffold(
        title = if (existingRule != null) "Edit Rule" else "Create Rule",
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            TextButton(
                onClick = {
                    if (ruleName.isNotBlank() && conditionValue.isNotBlank() && actionValue.isNotBlank()) {
                        val rule = TransactionRule(
                            id = existingRule?.id ?: UUID.randomUUID().toString(),
                            name = ruleName,
                            description = description.takeIf { it.isNotBlank() },
                            priority = 100,
                            conditions = listOf(
                                RuleCondition(
                                    field = selectedField,
                                    operator = selectedOperator,
                                    value = conditionValue
                                )
                            ),
                            actions = listOf(
                                RuleAction(
                                    field = actionField,
                                    actionType = ActionType.SET,
                                    value = actionValue
                                )
                            ),
                            isActive = true
                        )
                        onSaveRule(rule)
                        onNavigateBack()
                    }
                },
                enabled = ruleName.isNotBlank() && conditionValue.isNotBlank() && actionValue.isNotBlank()
            ) {
                Text("Save")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // Quick presets
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Quick Templates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonPresets.forEach { (label, action) ->
                            AssistChip(
                                onClick = action,
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }
                }
            }

            // Rule name and description
            OutlinedTextField(
                value = ruleName,
                onValueChange = { ruleName = it },
                label = { Text("Rule Name") },
                placeholder = { Text("e.g., Food expenses under ₹200") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("What does this rule do?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )

            // Condition section
            Card {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "When",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Field selector
                    ExposedDropdownMenuBox(
                        expanded = fieldDropdownExpanded,
                        onExpandedChange = { fieldDropdownExpanded = !fieldDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = when(selectedField) {
                                TransactionField.AMOUNT -> "Amount"
                                TransactionField.MERCHANT -> "Merchant"
                                TransactionField.CATEGORY -> "Category"
                                TransactionField.SMS_TEXT -> "SMS Text"
                                TransactionField.TYPE -> "Transaction Type"
                                else -> "Amount"
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Field") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = fieldDropdownExpanded,
                            onDismissRequest = { fieldDropdownExpanded = false }
                        ) {
                            listOf(
                                TransactionField.AMOUNT to "Amount",
                                TransactionField.MERCHANT to "Merchant",
                                TransactionField.SMS_TEXT to "SMS Text",
                                TransactionField.CATEGORY to "Category",
                                TransactionField.TYPE to "Transaction Type"
                            ).forEach { (field, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedField = field
                                        fieldDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Operator selector
                    val operators = when(selectedField) {
                        TransactionField.AMOUNT -> listOf(
                            ConditionOperator.LESS_THAN to "<",
                            ConditionOperator.GREATER_THAN to ">",
                            ConditionOperator.EQUALS to "="
                        )
                        else -> listOf(
                            ConditionOperator.CONTAINS to "contains",
                            ConditionOperator.EQUALS to "equals",
                            ConditionOperator.STARTS_WITH to "starts with"
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        operators.forEach { (op, label) ->
                            FilterChip(
                                selected = selectedOperator == op,
                                onClick = { selectedOperator = op },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Value input
                    OutlinedTextField(
                        value = conditionValue,
                        onValueChange = { conditionValue = it },
                        label = { Text("Value") },
                        placeholder = {
                            Text(
                                when(selectedField) {
                                    TransactionField.AMOUNT -> "e.g., 200"
                                    TransactionField.MERCHANT -> "e.g., Swiggy"
                                    TransactionField.SMS_TEXT -> "e.g., salary"
                                    else -> "Enter value"
                                }
                            )
                        },
                        keyboardOptions = if (selectedField == TransactionField.AMOUNT) {
                            KeyboardOptions(keyboardType = KeyboardType.Number)
                        } else {
                            KeyboardOptions.Default
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Action section
            Card {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Then",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "Set Category to:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Common categories as chips
                    val commonCategories = listOf(
                        "Food & Dining", "Transportation", "Shopping",
                        "Bills & Utilities", "Entertainment", "Healthcare",
                        "Investments", "Others"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        commonCategories.forEach { category ->
                            FilterChip(
                                selected = actionValue == category,
                                onClick = { actionValue = category },
                                label = { Text(category, style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    // Custom category input
                    OutlinedTextField(
                        value = actionValue,
                        onValueChange = { actionValue = it },
                        label = { Text("Or type custom category") },
                        placeholder = { Text("e.g., Rent") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Preview
            if (ruleName.isNotBlank() && conditionValue.isNotBlank() && actionValue.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = "Rule Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = buildString {
                                append("When ")
                                append(when(selectedField) {
                                    TransactionField.AMOUNT -> "amount"
                                    TransactionField.MERCHANT -> "merchant"
                                    TransactionField.SMS_TEXT -> "SMS text"
                                    TransactionField.CATEGORY -> "category"
                                    TransactionField.TYPE -> "type"
                                    else -> "field"
                                })
                                append(" ")
                                append(when(selectedOperator) {
                                    ConditionOperator.LESS_THAN -> "is less than"
                                    ConditionOperator.GREATER_THAN -> "is greater than"
                                    ConditionOperator.EQUALS -> "equals"
                                    ConditionOperator.CONTAINS -> "contains"
                                    ConditionOperator.STARTS_WITH -> "starts with"
                                    else -> "matches"
                                })
                                append(" ")
                                append(conditionValue)
                                append(", set category to ")
                                append(actionValue)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
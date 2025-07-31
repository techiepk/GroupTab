package com.example.pennywisecompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pennywisecompose.ui.theme.PennyWiseComposeTheme
import java.time.Instant
import java.util.UUID

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val merchant: String,
    val category: Category,
    val date: Instant = Instant.now()
)

enum class Category {
    FOOD, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, OTHER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Don't use enableEdgeToEdge() for now
        setContent {
            PennyWiseComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PennyWiseApp()
                }
            }
        }
    }
}

@Composable
fun PennyWiseApp(modifier: Modifier = Modifier) {
    var totalSpent by remember { mutableStateOf(0.0) }
    val transactions = remember {
        mutableStateListOf(
            Transaction(amount = 250.0, merchant = "Swiggy", category = Category.FOOD),
            Transaction(amount = 180.0, merchant = "Uber", category = Category.TRANSPORT),
            Transaction(amount = 1299.0, merchant = "Amazon", category = Category.SHOPPING)
        )
    }
    
    LaunchedEffect(transactions.toList()) {
        totalSpent = transactions.sumOf { it.amount }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Text(
            text = "PennyWise",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₹${String.format("%.2f", totalSpent)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        transactions.forEach { transaction ->
            TransactionCard(transaction = transaction)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = transaction.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "₹${String.format("%.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Theme")
@Composable
fun PennyWisePreviewLight() {
    PennyWiseComposeTheme(darkTheme = false) {
        PennyWiseApp()
    }
}

@Preview(showBackground = true, name = "Dark Theme")
@Composable
fun PennyWisePreviewDark() {
    PennyWiseComposeTheme(darkTheme = true) {
        PennyWiseApp()
    }
}

@Preview(showBackground = true)
@Composable
fun PennyWisePreview() {
    PennyWiseComposeTheme {
        PennyWiseApp()
    }
}
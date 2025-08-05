package com.pennywiseai.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun SpotlightTutorial(
    isVisible: Boolean,
    targetPosition: Rect?,
    message: String,
    onDismiss: () -> Unit
) {
    if (!isVisible || targetPosition == null) return
    
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "spotlight_alpha"
    )
    
    targetPosition?.let { rect ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
        ) {
                // Dark overlay with circular cutout for the target
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha }
                ) {
                    val path = Path().apply {
                        // First add the full screen rectangle
                        addRect(Rect(0f, 0f, size.width, size.height))
                        
                        // Then add the circular cutout for the FAB
                        val padding = 12.dp.toPx()
                        val center = rect.center
                        val radius = (rect.width / 2) + padding
                        
                        addOval(
                            Rect(
                                center.x - radius,
                                center.y - radius,
                                center.x + radius,
                                center.y + radius
                            )
                        )
                        
                        // Set fill type to EvenOdd to create the hole effect
                        fillType = PathFillType.EvenOdd
                    }
                    
                    // Draw the path with a dark color
                    drawPath(
                        path = path,
                        color = Color.Black.copy(alpha = 0.8f)
                    )
                }
                
                // Tutorial message positioned to the left of FAB
                val density = LocalDensity.current
                
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (rect.left - 220.dp.toPx()).toDp() },
                            y = with(density) { rect.center.y.toDp() - 30.dp }
                        )
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .width(200.dp)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap anywhere to dismiss",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Black.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * Modifier to capture the position of a composable for spotlight targeting
 */
fun Modifier.spotlightTarget(
    onPositioned: (Rect) -> Unit
) = this.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInWindow()
    onPositioned(bounds)
}
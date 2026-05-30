package org.example.frontend.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Curated Sleek Dark Palette
object PremiumTheme {
    val DeepSpaceBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF090D1A), // Dark Navy
            Color(0xFF131130), // Deep Indigo
            Color(0xFF030712)  // Near Black
        )
    )

    val GlassCardBg = Color(0x1C253457) // Translucent Slate Blue
    val GlowBorder = Brush.linearGradient(
        colors = listOf(
            Color(0x3BFFFFFF), // Glossy glow
            Color(0x08FFFFFF)  // Fades out
        )
    )

    // Accent Colors
    val NeonCyan = Color(0xFF06B6D4)
    val SoftPink = Color(0xFFEC4899)
    val EmeraldGreen = Color(0xFF10B981)
    val CoralRed = Color(0xFFEF4444)
    val AmethystPurple = Color(0xFF8B5CF6)
    
    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
    
    val CardShape = RoundedCornerShape(20.dp)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(16.dp, PremiumTheme.CardShape, clip = false)
            .background(PremiumTheme.GlassCardBg, PremiumTheme.CardShape)
            .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape)
            .padding(24.dp),
        content = content
    )
}

@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PremiumTheme.NeonCyan,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .shadow(8.dp, PremiumTheme.CardShape)
            .clip(PremiumTheme.CardShape)
            .background(if (enabled) color else Color(0x339CA3AF))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else Color(0x88F3F4F6),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp
        )
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isPassword: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = PremiumTheme.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = PremiumTheme.TextSecondary.copy(alpha = 0.6f)) },
            singleLine = true,
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PremiumTheme.TextPrimary,
                unfocusedTextColor = PremiumTheme.TextPrimary,
                focusedContainerColor = Color(0x10000000),
                unfocusedContainerColor = Color(0x05000000),
                focusedBorderColor = PremiumTheme.NeonCyan,
                unfocusedBorderColor = Color(0x22FFFFFF),
                cursorColor = PremiumTheme.NeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BaseScreenLayout(
    title: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = { AppState.goBack() },
    actionContent: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF090D1A),
            surface = Color(0xFF131130),
            primary = PremiumTheme.NeonCyan,
            secondary = PremiumTheme.AmethystPurple
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PremiumTheme.DeepSpaceBackground)
                .safeContentPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Custom App Bar
                if (title != null || showBackButton || actionContent != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showBackButton) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x1F293548))
                                        .clickable { onBackClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("<", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            if (title != null) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                        if (actionContent != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                actionContent()
                            }
                        }
                    }
                }

                // Global Alert / Alert Messages
                AnimatedVisibility(
                    visible = AppState.currentError != null || AppState.successMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val isError = AppState.currentError != null
                    val message = AppState.currentError ?: AppState.successMessage ?: ""
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isError) Color(0x33EF4444) else Color(0x3310B981))
                            .border(1.dp, if (isError) PremiumTheme.CoralRed else PremiumTheme.EmeraldGreen, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = message,
                                color = PremiumTheme.TextPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "[X]",
                                color = PremiumTheme.TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { AppState.clearMessages() }
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Screen main body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    content = content
                )
            }

            // Global Loading overlay
            if (AppState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC030712)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PremiumTheme.NeonCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ładowanie...", color = PremiumTheme.TextPrimary)
                    }
                }
            }
        }
    }
}

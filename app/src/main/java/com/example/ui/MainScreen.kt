package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBlueBright
import com.example.ui.theme.ImmersiveBluePrimary
import com.example.ui.theme.ImmersiveCardBg
import com.example.ui.theme.ImmersiveCyanAccent
import com.example.ui.theme.ImmersiveCyanGlow
import com.example.ui.theme.ImmersiveDarkBg
import com.example.ui.theme.ImmersiveOrangeAccent
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveTextMuted
import com.example.ui.theme.ImmersiveTextSubtle
import com.example.ui.theme.ImmersiveTextWhite
import com.example.viewmodel.AssistantUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    state: AssistantUiState,
    onMicClick: () -> Unit,
    onQuickCommand: (String) -> Unit,
    onConfirmAction: () -> Unit,
    onCancelAction: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    var showHistory by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(ImmersiveDarkBg),
            color = ImmersiveDarkBg
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Background Glowing Orb
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ImmersiveCyanGlow,
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Bar (Immersive UI style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Logo Badge [M]
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ImmersiveBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "M",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Munwar AI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ImmersiveTextWhite,
                                modifier = Modifier.testTag("app_title")
                            )
                        }

                        IconButton(
                            onClick = { showHistory = !showHistory },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ImmersiveCardBg)
                                .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                                .testTag("history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "ہسٹری",
                                tint = if (showHistory) ImmersiveCyanAccent else ImmersiveTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Offline Alert Banner
                    AnimatedVisibility(
                        visible = state.isOffline,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x33F97316)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, ImmersiveOrangeAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = ImmersiveOrangeAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "انٹرنیٹ دستیاب نہیں، صرف بنیادی کمانڈز کام کریں گی۔",
                                    fontSize = 12.sp,
                                    color = ImmersiveTextWhite
                                )
                            }
                        }
                    }

                    if (showHistory) {
                        // History View Panel
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                                .padding(vertical = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "سابقہ بات چیت",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveCyanAccent
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                if (state.interactionHistory.isEmpty()) {
                                    Text(
                                        text = "ابھی تک کوئی ہسٹری موجود نہیں ہے۔",
                                        color = ImmersiveTextMuted,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(state.interactionHistory) { item ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Color(0x22FFFFFF),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "آپ: ${item.userQuery}",
                                                    color = ImmersiveCyanAccent,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Munwar AI: ${item.assistantReply}",
                                                    color = ImmersiveTextWhite,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Center Immersive Stage
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Immersive Glowing Microphone Orb
                            ImmersiveMicButton(
                                isListening = state.isListening,
                                isProcessing = state.isProcessing,
                                onClick = onMicClick
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Greeting & Speech Reply Text Display
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.userSpeech.isNotBlank()) {
                                    Surface(
                                        color = Color(0x1A3B82F6),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .padding(bottom = 12.dp)
                                            .border(1.dp, Color(0x333B82F6), RoundedCornerShape(16.dp))
                                    ) {
                                        Text(
                                            text = "آپ: ${state.userSpeech}",
                                            fontSize = 14.sp,
                                            color = ImmersiveCyanAccent,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = state.assistantResponse,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveTextWhite,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 28.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = state.statusText,
                                    fontSize = 13.sp,
                                    color = ImmersiveTextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Footer Card (Equalizer Visualizer + Quick Suggestion Chips)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(28.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Soundwave Waveform Equalizer
                            AudioWaveformVisualizer(
                                isActive = state.isListening || state.isSpeaking || state.isProcessing
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Quick Suggestions Row
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ImmersiveQuickChip(
                                    text = "\"یوٹیوب پر قوالی چلا دو\"",
                                    icon = Icons.Default.PlayArrow,
                                    badgeColor = ImmersiveBluePrimary
                                ) { onQuickCommand("یوٹیوب پر قوالی چلا دو") }

                                ImmersiveQuickChip(
                                    text = "\"فلیش لائٹ آن کرو\"",
                                    icon = Icons.Default.FlashOn,
                                    badgeColor = ImmersiveOrangeAccent
                                ) { onQuickCommand("فلیش لائٹ آن کرو") }

                                ImmersiveQuickChip(
                                    text = "\"سیٹنگز کھولو\"",
                                    icon = Icons.Default.Settings,
                                    badgeColor = ImmersiveCyanAccent
                                ) { onQuickCommand("وائی فائی سیٹنگز کھولو") }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "بات کرنے کے لیے مائیک دبائیں",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveTextSubtle,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // Action Confirmation Dialog
                if (state.pendingConfirmationCommand != null) {
                    AlertDialog(
                        onDismissRequest = onCancelAction,
                        title = {
                            Text(
                                text = "تصدیق کی ضرورت ہے",
                                color = ImmersiveCyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = state.assistantResponse,
                                color = ImmersiveTextWhite,
                                fontSize = 16.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = onConfirmAction,
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBluePrimary),
                                modifier = Modifier.testTag("confirm_action_button")
                            ) {
                                Text("ہاں، مکمل کریں", color = Color.White)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = onCancelAction,
                                modifier = Modifier.testTag("cancel_action_button")
                            ) {
                                Text("نہیں، منسوخ کریں", color = ImmersiveTextWhite)
                            }
                        },
                        containerColor = ImmersiveSurface,
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ImmersiveMicButton(
    isListening: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Concentric Circular Frame Layout matching HTML design
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(210.dp)
    ) {
        // Outer Frame Ring
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(scale)
                .clip(CircleShape)
                .border(2.dp, Color(0x4D3B82F6), CircleShape)
                .padding(14.dp)
        ) {
            // Middle Translucent Ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0x660F172A))
                    .border(1.dp, Color(0x3338BDF8), CircleShape)
                    .padding(14.dp)
            ) {
                // Inner Microphone Gradient Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (isListening) {
                                    listOf(ImmersiveOrangeAccent, Color(0xFFEA580C))
                                } else {
                                    listOf(ImmersiveBluePrimary, ImmersiveCyanAccent)
                                }
                            )
                        )
                        .clickable { onClick() }
                        .testTag("mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                        contentDescription = "مائیکرو فون",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioWaveformVisualizer(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bars")

    val heights = listOf(12.dp, 24.dp, 40.dp, 28.dp, 16.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        heights.forEachIndexed { index, defaultHeight ->
            val animatedFactor by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 350 + (index * 120),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            val currentHeight = if (isActive) defaultHeight * animatedFactor else defaultHeight * 0.5f

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(currentHeight)
                    .clip(CircleShape)
                    .background(
                        if (index == 2) ImmersiveBlueBright else ImmersiveBluePrimary.copy(alpha = 0.7f)
                    )
            )
        }
    }
}

@Composable
fun ImmersiveQuickChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0x0DFFFFFF),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = ImmersiveTextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

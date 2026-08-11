package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainScreen
import com.example.ui.theme.ImmersiveBluePrimary
import com.example.ui.theme.ImmersiveCyanAccent
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.ImmersiveTextWhite
import com.example.ui.theme.MunwarAiTheme
import com.example.viewmodel.AssistantViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            viewModel.initSpeechRecognizer(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            viewModel.initSpeechRecognizer(this)
        } else {
            requestRequiredPermissions()
        }

        setContent {
            MunwarAiTheme {
                val uiState by viewModel.uiState.collectAsState()
                var showPermissionDialog by remember { mutableStateOf(!hasPermission(Manifest.permission.RECORD_AUDIO)) }

                MainScreen(
                    state = uiState,
                    onMicClick = {
                        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
                            viewModel.onMicrophoneClick()
                        } else {
                            showPermissionDialog = true
                        }
                    },
                    onQuickCommand = { text ->
                        viewModel.processVoiceCommand(text)
                    },
                    onConfirmAction = { viewModel.confirmPendingAction() },
                    onCancelAction = { viewModel.cancelPendingAction() },
                    onRequestPermissions = { requestRequiredPermissions() }
                )

                if (showPermissionDialog) {
                    PermissionOnboardingDialog(
                        onDismiss = { showPermissionDialog = false },
                        onRequest = {
                            showPermissionDialog = false
                            requestRequiredPermissions()
                        }
                    )
                }
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRequiredPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }
}

@Composable
fun PermissionOnboardingDialog(
    onDismiss: () -> Unit,
    onRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "اجازتیں درکار ہیں",
                color = ImmersiveCyanAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = "Munwar AI کو بہترین طریقے سے آواز کے ذریعے کام کرنے کے لیے ان اجازتوں کی ضرورت ہے:\n\n" +
                        "🎤 مائیکروفون: آپ کی اردو آواز سننے کے لیے\n" +
                        "👤 Contacts & Phone: رابطہ تلاش کر کے کال کرنے کے لیے\n" +
                        "📷 کیمرا: فلیش لائٹ اور کیمرا کھولنے کے لیے",
                color = ImmersiveTextWhite,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBluePrimary)
            ) {
                Text("اجازتیں فراہم کریں", color = Color.White)
            }
        },
        containerColor = ImmersiveSurface
    )
}

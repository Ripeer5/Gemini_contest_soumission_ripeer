package com.artalk.ripeer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.artalk.ripeer.data.repository.TextToSpeechRepository
import com.artalk.ripeer.ui.auth.AuthenticationApp
import com.artalk.ripeer.ui.chat.ChatScreen
import com.artalk.ripeer.ui.theme.ArtalkTheme
import com.artalk.ripeer.ui.users.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var microphonePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var ttsRepository: TextToSpeechRepository

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        microphonePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                // Handle the case where the permission is not granted
            }
        }

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                // Handle the case where the permission is not granted
            }
        }

        setContent {
            val user by userViewModel.user.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            val apiKey = "3eb380225e720c0f84c370e6157c2bf4"
            val voiceId = "nPczCjzI2devNBz1zQrb"

            ArtalkTheme {
                if (user == null) {
                    AuthenticationApp()
                } else {
                    ChatScreen(
                        mainViewModel = mainViewModel,
                        user = user!!,
                        onLogoutClicked = {
                            FirebaseAuth.getInstance().signOut()
                            userViewModel.updateUser(null)
                        },
                        apiKey = apiKey,
                        voiceId = voiceId
                    )
                }
            }
        }
    }
}

package com.artalk.ripeer.ui.chat

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artalk.ripeer.MainViewModel
import com.artalk.ripeer.PortraitCaptureActivity
import com.artalk.ripeer.ui.common.AppBar
import com.artalk.ripeer.ui.common.AppScaffold
import com.artalk.ripeer.ui.conversations.Conversation
import com.artalk.ripeer.ui.conversations.ConversationViewModel
import com.artalk.ripeer.ui.conversations.ui.theme.ArtalkTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    mainViewModel: MainViewModel,
    user: FirebaseUser,
    onLogoutClicked: () -> Unit,
    apiKey: String,
    voiceId: String
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerOpen by mainViewModel.drawerShouldBeOpened.collectAsState()
    val context = LocalContext.current
    val conversationViewModel: ConversationViewModel = hiltViewModel()

    val qrScanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val resultCode = result.resultCode
        val intentResult = IntentIntegrator.parseActivityResult(resultCode, data)
        if (intentResult != null) {
            if (intentResult.contents == null) {
                Toast.makeText(context, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val artworkId = intentResult.contents
                fetchArtworkAndStartConversation(context, artworkId, conversationViewModel)
            }
        }
    }

    if (drawerOpen) {
        LaunchedEffect(Unit) {
            try {
                drawerState.open()
            } finally {
                mainViewModel.resetOpenDrawerAction()
            }
        }
    }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    BackHandler {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        } else {
            focusManager.clearFocus()
        }
    }

    val darkTheme = remember { mutableStateOf(true) }
    ArtalkTheme(darkTheme.value) {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            AppScaffold(
                drawerState = drawerState,
                onChatClicked = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onNewChatClicked = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                onIconClicked = {
                    darkTheme.value = !darkTheme.value
                },
                conversationViewModel = conversationViewModel,
                onLogoutClicked = onLogoutClicked,
                onQrCodeScanned = {
                    val intentIntegrator = IntentIntegrator(context as androidx.activity.ComponentActivity)
                    intentIntegrator.setPrompt("Scan a QR code")
                    intentIntegrator.captureActivity = PortraitCaptureActivity::class.java
                    qrScanLauncher.launch(intentIntegrator.createScanIntent())
                }
            ) {
                val conversations by conversationViewModel.conversationsState.collectAsState()

                if (conversations.isEmpty()) {
                    EmptyChatScreen(
                        onQrCodeScanned = {
                            val intentIntegrator = IntentIntegrator(context as androidx.activity.ComponentActivity)
                            intentIntegrator.setPrompt("Scan a QR code")
                            intentIntegrator.captureActivity = PortraitCaptureActivity::class.java
                            qrScanLauncher.launch(intentIntegrator.createScanIntent())
                        },
                        onLogoutClicked = onLogoutClicked
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AppBar(onClickMenu = {
                            scope.launch {
                                drawerState.open()
                            }
                        }, userName = user.displayName ?: user.email ?: user.uid ?: "Unknown")
                        Divider()
                        Conversation(apiKey = apiKey, voiceId = voiceId)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatScreen(onQrCodeScanned: () -> Unit, onLogoutClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No conversations found",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onQrCodeScanned) {
            Text("Click here to scan your first QR code")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogoutClicked) {
            Text("Logout")
        }
    }
}

fun fetchArtworkAndStartConversation(
    context: Context,
    artworkId: String,
    conversationViewModel: ConversationViewModel
) {
    val db = FirebaseFirestore.getInstance()
    db.collection("artworks").document(artworkId).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val title = document.getString("title") ?: "Untitled"
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                val artworkId = document.getString("id") ?: "BadArtworkId"
                val collectionName = document.getString("collection_name") ?: "BadCollectionName"

                conversationViewModel.checkIfConversationExists(userId, artworkId) { exists ->
                    if (exists) {
                        Toast.makeText(context, "Vous avez déjà une conversation avec l'artwork: $title", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Artwork: $title", Toast.LENGTH_LONG).show()
                        conversationViewModel.newConversation(title, artworkId, collectionName)
                    }
                }
            } else {
                Toast.makeText(context, "No artwork found with ID: $artworkId", Toast.LENGTH_LONG).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error fetching artwork: ${e.message}", Toast.LENGTH_LONG).show()
        }
}

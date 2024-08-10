package com.artalk.ripeer.ui.common

import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue.Closed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.artalk.ripeer.data.remote.ConversationRepository
import com.artalk.ripeer.data.remote.MessageRepository
import com.artalk.ripeer.data.remote.TopicBackendRepository
import com.artalk.ripeer.data.repository.TextToSpeechRepository
import com.artalk.ripeer.ui.conversations.ConversationViewModel
import com.artalk.ripeer.ui.theme.BackGroundColor
import com.artalk.ripeer.models.ConversationModel
import com.artalk.ripeer.models.MessageModel
import com.artalk.ripeer.models.TextToSpeechRequest
import com.artalk.ripeer.models.TopicTextCompletionParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    drawerState: DrawerState = rememberDrawerState(initialValue = Closed),
    onChatClicked: (String) -> Unit,
    onNewChatClicked: () -> Unit,
    onIconClicked: () -> Unit = {},
    conversationViewModel: ConversationViewModel = hiltViewModel(),
    onLogoutClicked: () -> Unit,
    onQrCodeScanned: () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    scope.launch {
        conversationViewModel.initialize()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = BackGroundColor) {
                AppDrawer(
                    onChatClicked = onChatClicked,
                    onNewChatClicked = onNewChatClicked,
                    onIconClicked = onIconClicked,
                    onLogoutClicked = onLogoutClicked,
                    onQrCodeScanned = onQrCodeScanned
                )
            }
        },
        content = content
    )
}



// Définissez des classes factices pour les dépendances de ConversationViewModel
class FakeConversationRepository : ConversationRepository {
    override suspend fun fetchConversations(userId: String): MutableList<ConversationModel> {
        return mutableListOf()
    }

    override fun newConversation(conversation: ConversationModel): ConversationModel {
        return conversation
    }

    override suspend fun deleteConversation(conversationId: String, userId: String) {
        // Implémentation factice pour l'aperçu
    }
}

class FakeMessageRepository : MessageRepository {
    override fun fetchMessages(conversationId: String): Flow<List<MessageModel>> {
        return flowOf(emptyList())
    }

    override fun createMessage(message: MessageModel): MessageModel {
        return message
    }

    override suspend fun deleteMessagesByConversation(conversationId: String) {
        // Implémentation factice pour l'aperçu
    }
}

class FakeTopicBackendRepository : TopicBackendRepository {
    override fun getMessage(param: TopicTextCompletionParam): Flow<String> {
        return flowOf("Fake response")
    }
}

class FakeTTSRepository : TextToSpeechRepository {
    override suspend fun convertTextToSpeech(
        apiKey: String,
        voiceId: String,
        request: TextToSpeechRequest
    ): ResponseBody {
        return ResponseBody.create(null, ByteArray(0))
    }

    override fun playAudio(responseBody: ResponseBody, messageId: String) {
        // Implémentation factice pour l'aperçu
    }

    override fun releasePlayer() {
        // Implémentation factice pour l'aperçu
    }

    override fun pauseAudio() {
        // Implémentation factice pour l'aperçu
    }

    override fun isPlaying(): Boolean {
        return false
    }

    override fun isAudioCached(messageId: String): Boolean {
        return false
    }

    override fun playCachedAudio(messageId: String) {
        // Implémentation factice pour l'aperçu
    }
}

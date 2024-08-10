package com.artalk.ripeer.ui.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artalk.ripeer.data.remote.ConversationRepository
import com.artalk.ripeer.data.remote.MessageRepository
import com.artalk.ripeer.data.remote.TopicBackendRepository
import com.artalk.ripeer.data.repository.TextToSpeechRepository
import com.artalk.ripeer.helpers.FirestoreHelper
import com.artalk.ripeer.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ConversationViewModel @Inject constructor(
    application: Application,
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val topicBackendRepository: TopicBackendRepository,
    private val ttsRepository: TextToSpeechRepository
) : AndroidViewModel(application) {

    private val _currentConversation: MutableStateFlow<String> = MutableStateFlow("")
    private val _conversations: MutableStateFlow<MutableList<ConversationModel>> = MutableStateFlow(mutableListOf())
    private val _messages: MutableStateFlow<HashMap<String, MutableList<MessageModel>>> = MutableStateFlow(HashMap())
    private val _isFetching: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _isFabExpanded: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private var isAudioPaused = false

    val currentConversationState: StateFlow<String> = _currentConversation.asStateFlow()
    val conversationsState: StateFlow<MutableList<ConversationModel>> = _conversations.asStateFlow()
    val messagesState: StateFlow<HashMap<String, MutableList<MessageModel>>> = _messages.asStateFlow()
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()
    val isFabExpanded: StateFlow<Boolean> get() = _isFabExpanded

    private var stopReceivingResults: Boolean = false

    fun toggleAudioPlayback(apiKey: String, voiceId: String, messageId: String, text: String) {
        viewModelScope.launch {
            try {
                if (ttsRepository.isPlaying()) {
                    Log.d("TextToSpeech", "Audio is currently playing. Pausing audio.")
                    ttsRepository.pauseAudio()
                    isAudioPaused = true
                } else if (isAudioPaused) {
                    Log.d("TextToSpeech", "Audio is paused. Resuming audio.")
                    ttsRepository.pauseAudio() // This will resume the audio
                    isAudioPaused = false
                } else {
                    if (ttsRepository.isAudioCached(messageId)) {
                        Log.d("TextToSpeech", "Playing cached audio for messageId: $messageId")
                        ttsRepository.playCachedAudio(messageId)
                    } else {
                        Log.d("TextToSpeech", "Audio is not cached. Starting new audio playback.")
                        val request = TextToSpeechRequest(text = text)
                        Log.d("TextToSpeech", "Sending request: $request")
                        val response = ttsRepository.convertTextToSpeech(apiKey, voiceId, request)
                        Log.d("TextToSpeech", "Received response: $response")
                        ttsRepository.playAudio(response, messageId)
                        Log.d("TextToSpeech", "Audio playback started")
                    }
                }
            } catch (e: Exception) {
                Log.e("TextToSpeech", "Error during text-to-speech: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsRepository.releasePlayer()
    }

    fun stopReceivingResults() {
        stopReceivingResults = true
    }

    suspend fun initialize() {
        _isFetching.value = true

        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _conversations.value = conversationRepo.fetchConversations(userId)

        if (_conversations.value.isNotEmpty()) {
            _currentConversation.value = _conversations.value.first().id
            fetchMessages()
        }

        _isFetching.value = false
    }

    fun checkIfConversationExists(userId: String, artworkId: String, callback: (Boolean) -> Unit) {
        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
        db.collection("conversations")
            .whereEqualTo("userId", userId)
            .whereEqualTo("artworkId", artworkId)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.isEmpty.not())
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    suspend fun onConversation(conversation: ConversationModel) {
        _isFetching.value = true
        _currentConversation.value = conversation.id

        fetchMessages()
        _isFetching.value = false
    }

    suspend fun sendMessage(message: String) {
        stopReceivingResults = false
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (getMessagesByConversation(_currentConversation.value).isEmpty()) {
            createConversationRemote(message, userId)
        }

        val newMessageModel = MessageModel(
            question = message,
            answer = "Let me thinking...",
            conversationId = _currentConversation.value,
            createdAt = Date()
        )

        val currentListMessage: MutableList<MessageModel> = getMessagesByConversation(_currentConversation.value).toMutableList()

        // Insert message to list
        currentListMessage.add(0, newMessageModel)
        setMessages(currentListMessage)

        // Save to Firestore
        messageRepo.createMessage(newMessageModel)
    }

    suspend fun sendMessageToCustomApi(message: String) {
        stopReceivingResults = false
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val currentConversationId: String = _currentConversation.value

        val newMessageModel = MessageModel(
            question = message,
            answer = "Let me thinking...",
            conversationId = currentConversationId,
            createdAt = Date()
        )

        val currentListMessage: MutableList<MessageModel> = getMessagesByConversation(currentConversationId).toMutableList()
        currentListMessage.add(0, newMessageModel)
        setMessages(currentListMessage)

        val collectionName: String = FirestoreHelper.getCollectionName(currentConversationId) ?: "defaultCollectionName"

        val currentConversation: ConversationModel? = _conversations.value.find { it.id == currentConversationId }
        val collectionNameFromConversation: String = currentConversation?.collectionName ?: collectionName

        val flow: Flow<String> = topicBackendRepository.getMessage(
            TopicTextCompletionParam(
                query = getPrompt(currentConversationId),
                collection_name = collectionNameFromConversation,
                prompt_template = "prompt_template_TOPIC"
            )
        )

        var answerFromCustomApi: String = ""
        flow.onCompletion {
            if (answerFromCustomApi.isBlank()) {
                answerFromCustomApi = "Désolé, je n'ai pas de réponse pour le moment."
                updateLocalAnswer(answerFromCustomApi.trim(), currentConversationId)
            }
            setFabExpanded(false)
        }.collect { value ->
            if (stopReceivingResults) {
                setFabExpanded(false)
                return@collect
            }
            answerFromCustomApi += value
            updateLocalAnswer(answerFromCustomApi.trim(), currentConversationId)
            setFabExpanded(true)
        }

        messageRepo.createMessage(newMessageModel.copy(answer = answerFromCustomApi))
    }

    private fun createConversationRemote(title: String, userId: String, artworkId: String = "", collectionName: String = "") {
        val newConversation = ConversationModel(
            id = _currentConversation.value,
            title = title,
            userId = userId,
            createdAt = Date(),
            artworkId = artworkId,
            collectionName = collectionName
        )

        conversationRepo.newConversation(newConversation)

        val conversations: MutableList<ConversationModel> = _conversations.value.toMutableList()
        conversations.add(0, newConversation)

        _conversations.value = conversations
    }

    fun newConversation(title: String = "", artworkId: String = "", collectionName: String = "") {
        val conversationId: String = UUID.randomUUID().toString()
        _currentConversation.value = conversationId

        if (title.isNotEmpty() && artworkId.isNotEmpty()) {
            val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: return
            createConversationRemote(title, userId, artworkId, collectionName)
        }
    }

    private fun getMessagesByConversation(conversationId: String): MutableList<MessageModel> {
        return _messages.value[conversationId] ?: mutableListOf()
    }

    suspend fun deleteConversation(conversationId: String) {
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: return
        conversationRepo.deleteConversation(conversationId, userId)
        messageRepo.deleteMessagesByConversation(conversationId)

        val conversations: MutableList<ConversationModel> = _conversations.value.toMutableList()
        val conversationToRemove: ConversationModel? = conversations.find { it.id == conversationId }

        if (conversationToRemove != null) {
            conversations.remove(conversationToRemove)
            _conversations.value = conversations
        }
    }

    private suspend fun fetchMessages() {
        if (_currentConversation.value.isEmpty() || _messages.value[_currentConversation.value] != null) return

        val flow: Flow<List<MessageModel>> = messageRepo.fetchMessages(_currentConversation.value)
        flow.collectLatest { setMessages(it.toMutableList()) }
    }

    private fun setMessages(messages: MutableList<MessageModel>) {
        val messagesMap: HashMap<String, MutableList<MessageModel>> = _messages.value.clone() as HashMap<String, MutableList<MessageModel>>
        messagesMap[_currentConversation.value] = messages
        _messages.value = messagesMap
    }

    private fun getPrompt(conversationId: String): String {
        val messagesMap: HashMap<String, MutableList<MessageModel>> = _messages.value.clone() as HashMap<String, MutableList<MessageModel>>

        var response: String = ""

        for (message in messagesMap[conversationId]!!.reversed()) {
            response += """Human:${message.question.trim()}
                |Bot:${
                if (message.answer == "Let me thinking...") ""
                else message.answer.trim()
            }""".trimMargin()
        }

        return response
    }

    private fun updateLocalAnswer(answer: String, conversationId: String) {
        val currentListMessage: MutableList<MessageModel> = getMessagesByConversation(conversationId).toMutableList()
        currentListMessage[0] = currentListMessage[0].copy(answer = answer)
        setMessages(currentListMessage)
    }

    private fun setFabExpanded(expanded: Boolean) {
        _isFabExpanded.value = expanded
    }
}

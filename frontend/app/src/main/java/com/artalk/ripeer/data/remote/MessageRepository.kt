package com.artalk.ripeer.data.remote

import com.artalk.ripeer.models.MessageModel
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun fetchMessages(conversationId: String): Flow<List<MessageModel>>
    fun createMessage(message: MessageModel): MessageModel
    suspend fun deleteMessagesByConversation(conversationId: String)
}

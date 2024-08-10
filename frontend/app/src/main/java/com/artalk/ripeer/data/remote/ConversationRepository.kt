package com.artalk.ripeer.data.remote

import com.artalk.ripeer.models.ConversationModel

interface ConversationRepository {
    suspend fun fetchConversations(userId: String): MutableList<ConversationModel>
    fun newConversation(conversation: ConversationModel): ConversationModel
    suspend fun deleteConversation(conversationId: String, userId: String)
}

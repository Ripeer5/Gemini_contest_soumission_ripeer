package com.artalk.ripeer.data.remote

import com.artalk.ripeer.models.ConversationModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreConversationRepository : ConversationRepository {
    private val db = FirebaseFirestore.getInstance()

    override suspend fun fetchConversations(userId: String): MutableList<ConversationModel> {
        val result = db.collection("conversations")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return result.toObjects(ConversationModel::class.java)
    }

    override fun newConversation(conversation: ConversationModel): ConversationModel {
        db.collection("conversations").document(conversation.id).set(conversation)
        return conversation
    }

    override suspend fun deleteConversation(conversationId: String, userId: String) {
        val conversationRef = db.collection("conversations").document(conversationId)
        conversationRef.get().await().let { document ->
            if (document.exists() && document.getString("userId") == userId) {
                conversationRef.delete().await()
            }
        }
    }
}
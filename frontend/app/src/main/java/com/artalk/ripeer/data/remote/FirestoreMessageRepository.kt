package com.artalk.ripeer.data.remote

import com.artalk.ripeer.models.MessageModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirestoreMessageRepository : MessageRepository {
    private val db = FirebaseFirestore.getInstance()

    override fun fetchMessages(conversationId: String): Flow<List<MessageModel>> = flow {
        val result = db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()
        emit(result.toObjects(MessageModel::class.java))
    }

    override fun createMessage(message: MessageModel): MessageModel {
        db.collection("messages").document(message.id).set(message)
        return message
    }

    override suspend fun deleteMessagesByConversation(conversationId: String) {
        val result = db.collection("messages")
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()
        for (document in result.documents) {
            document.reference.delete().await()
        }
    }
}
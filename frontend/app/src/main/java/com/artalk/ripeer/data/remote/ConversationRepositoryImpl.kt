package com.artalk.ripeer.data.remote

import com.artalk.ripeer.constants.conversationCollection
import com.artalk.ripeer.models.ConversationModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val fsInstance: FirebaseFirestore,
) : ConversationRepository {

    override suspend fun fetchConversations(userId: String): MutableList<ConversationModel> {
        val snapshot = fsInstance.collection(conversationCollection)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return if (snapshot.documents.isNotEmpty()) {
            snapshot.documents.mapNotNull { it.toObject(ConversationModel::class.java) }.toMutableList()
        } else {
            mutableListOf()
        }
    }

    override fun newConversation(conversation: ConversationModel): ConversationModel {
        fsInstance.collection(conversationCollection).document(conversation.id).set(conversation)
        return conversation
    }

    override suspend fun deleteConversation(conversationId: String, userId: String) {
        val snapshot = fsInstance.collection(conversationCollection)
            .whereEqualTo("id", conversationId)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        if (snapshot.documents.isNotEmpty()) {
            snapshot.documents.forEach { doc ->
                fsInstance.collection(conversationCollection).document(doc.id).delete().await()
            }
        }
    }
}

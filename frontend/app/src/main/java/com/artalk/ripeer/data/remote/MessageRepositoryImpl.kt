package com.artalk.ripeer.data.remote

import com.artalk.ripeer.constants.messageCollection
import com.artalk.ripeer.models.MessageModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val fsInstance: FirebaseFirestore,
) : MessageRepository {

    override fun fetchMessages(conversationId: String): Flow<List<MessageModel>> = callbackFlow {
        val listenerRegistration = fsInstance
            .collection(messageCollection)
            .whereEqualTo("conversationId", conversationId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshots?.documents?.mapNotNull { it.toObject(MessageModel::class.java) } ?: listOf()
                trySend(messages)
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun createMessage(message: MessageModel): MessageModel {
        fsInstance.collection(messageCollection).document(message.id).set(message)
        return message
    }

    override suspend fun deleteMessagesByConversation(conversationId: String) {
        val result = fsInstance
            .collection(messageCollection)
            .whereEqualTo("conversationId", conversationId)
            .get()
            .await()

        for (document in result.documents) {
            fsInstance.collection(messageCollection).document(document.id).delete().await()
        }
    }
}

package com.artalk.ripeer.helpers

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirestoreHelper {
    suspend fun getCollectionName(conversationId: String): String? {
        val db = FirebaseFirestore.getInstance()
        return suspendCancellableCoroutine { continuation ->
            val conversationRef = db.collection("conversations").document(conversationId)
            conversationRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val collectionName = document.getString("collectionName")
                    continuation.resume(collectionName)
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }
}

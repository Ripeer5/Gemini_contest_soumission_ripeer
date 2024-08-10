package com.artalk.ripeer.data.remote

import com.artalk.ripeer.data.api.TopicBackendApi
import com.artalk.ripeer.models.TopicTextCompletionParam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TopicBackendRepositoryImpl @Inject constructor(
    private val topicBackendApi: TopicBackendApi
): TopicBackendRepository{
    override fun getMessage(params: TopicTextCompletionParam): Flow<String> = callbackFlow {
        withContext(Dispatchers.IO) {
            try {
                val response = topicBackendApi.getMessage(params).execute()

                if (response.isSuccessful) {
                    response.body()?.byteStream()?.bufferedReader()?.use { reader ->
                        reader.lineSequence().forEach { line ->
                            trySend(line).isSuccess
                        }
                    }
                } else {
                    trySend("Failure! Try again")
                }
            } catch (e: Exception) {
                trySend("Failure due to exception: ${e.message}")
            } finally {
                close()
            }
        }
    }
}
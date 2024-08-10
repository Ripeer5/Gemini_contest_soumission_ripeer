package com.artalk.ripeer.data.remote

import com.artalk.ripeer.models.TopicTextCompletionParam
import kotlinx.coroutines.flow.Flow

interface TopicBackendRepository {
    fun getMessage(params: TopicTextCompletionParam): Flow<String>
}
package com.artalk.ripeer.data.api

import com.artalk.ripeer.constants.textCompletionsTopicEndpoint
import com.artalk.ripeer.models.TopicTextCompletionParam
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface TopicBackendApi {
    @POST(textCompletionsTopicEndpoint)
    @Streaming
    fun getMessage(@Body params: TopicTextCompletionParam): Call<ResponseBody>
}
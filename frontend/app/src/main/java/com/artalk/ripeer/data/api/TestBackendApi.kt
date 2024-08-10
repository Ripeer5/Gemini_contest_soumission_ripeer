package com.artalk.ripeer.data.api

import com.artalk.ripeer.constants.textCompletionsTopicEndpoint
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.POST

interface TestBackendApi {
    @POST(textCompletionsTopicEndpoint)
    fun getMessage(): Call<JsonObject>
}
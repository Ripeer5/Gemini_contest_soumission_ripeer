package com.artalk.ripeer.data.api

import com.artalk.ripeer.models.TextToSpeechRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ElevenlabsAPI {
    @POST("v1/text-to-speech/{voice_id}")
    suspend fun textToSpeech(
        @Header("xi-api-key") apiKey: String,
        @Path("voice_id") voiceId: String,
        @Body request: TextToSpeechRequest
    ): ResponseBody
}
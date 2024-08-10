package com.artalk.ripeer.data.repository

import com.artalk.ripeer.models.TextToSpeechRequest
import okhttp3.ResponseBody

interface TextToSpeechRepository {
    suspend fun convertTextToSpeech(apiKey: String, voiceId: String, request: TextToSpeechRequest): ResponseBody
    fun playAudio(responseBody: ResponseBody, messageId: String)
    fun pauseAudio()
    fun releasePlayer()
    fun isPlaying(): Boolean
    fun isAudioCached(messageId: String): Boolean
    fun playCachedAudio(messageId: String)
}

package com.artalk.ripeer.data.repository

import android.content.Context
import android.net.Uri
import com.artalk.ripeer.data.api.ElevenlabsAPI
import com.artalk.ripeer.models.TextToSpeechRequest
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject
import android.util.Log

class TextToSpeechRepositoryImpl @Inject constructor(
    private val context: Context,
    private val api: ElevenlabsAPI
) : TextToSpeechRepository {

    private var player: ExoPlayer? = null
    private var isPaused = false
    private val audioCache = mutableMapOf<String, File>()

    override suspend fun convertTextToSpeech(apiKey: String, voiceId: String, request: TextToSpeechRequest): ResponseBody {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TextToSpeechRepository", "Making API call with voiceId: $voiceId")
                api.textToSpeech(apiKey, voiceId, request)
            } catch (e: Exception) {
                Log.e("TextToSpeechRepository", "Error making API call: ${e.message}", e)
                throw e
            }
        }
    }

    override fun playAudio(responseBody: ResponseBody, messageId: String) {
        try {
            val tempFile = File.createTempFile("tts_$messageId", ".mp3", context.cacheDir)
            tempFile.writeBytes(responseBody.bytes())
            audioCache[messageId] = tempFile
            playAudioFromFile(tempFile)
        } catch (e: Exception) {
            Log.e("TextToSpeechRepository", "Error during audio playback: ${e.message}", e)
        }
    }

    private fun playAudioFromFile(file: File) {
        try {
            if (player == null) {
                player = ExoPlayer.Builder(context).build()
            }

            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            player!!.setMediaItem(mediaItem)
            player!!.prepare()
            player!!.playWhenReady = true
            isPaused = false
            Log.d("TextToSpeechRepository", "Audio playback started from file")
        } catch (e: Exception) {
            Log.e("TextToSpeechRepository", "Error during audio playback from file: ${e.message}", e)
        }
    }

    override fun pauseAudio() {
        player?.let {
            if (it.isPlaying) {
                it.playWhenReady = false
                isPaused = true
                Log.d("TextToSpeechRepository", "Audio paused")
            } else {
                if (isPaused) {
                    it.playWhenReady = true
                    isPaused = false
                    Log.d("TextToSpeechRepository", "Audio resumed")
                } else {
                    Log.d("TextToSpeechRepository", "Audio is neither playing nor paused")
                }
            }
        } ?: run {
            Log.d("TextToSpeechRepository", "Player is null")
        }
    }

    override fun releasePlayer() {
        player?.release()
        player = null
        Log.d("TextToSpeechRepository", "Player released")
    }

    override fun isPlaying(): Boolean {
        val playing = player?.isPlaying ?: false
        Log.d("TextToSpeechRepository", "isPlaying: $playing")
        return playing
    }

    override fun isAudioCached(messageId: String): Boolean {
        return audioCache.containsKey(messageId)
    }

    override fun playCachedAudio(messageId: String) {
        audioCache[messageId]?.let {
            playAudioFromFile(it)
        }
    }
}

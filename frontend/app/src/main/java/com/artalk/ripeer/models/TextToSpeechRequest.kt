package com.artalk.ripeer.models

data class TextToSpeechRequest(
    val text: String,
    val model_id: String = "eleven_multilingual_v2",
    val voice_settings: VoiceSettings = VoiceSettings(
        stability = 0.8f,
        similarity_boost = 0.75f,
        style = 0.1f,
        use_speaker_boost = false
    )
)

data class VoiceSettings(
    val stability: Float,
    val similarity_boost: Float,
    val style: Float,
    val use_speaker_boost: Boolean
)

data class PronunciationDictionaryLocator(
    val pronunciation_dictionary_id: String,
    val version_id: String
)

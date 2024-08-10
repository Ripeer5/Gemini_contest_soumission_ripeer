package com.artalk.ripeer.models

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


data class TestTextCompletionParam(
    @SerializedName("prompt")
    val promptText: String = ""
) {
    fun toJson(): JsonObject {
        val json = JsonObject()
        json.addProperty("prompt", promptText)
        return json
    }
}
package com.artalk.ripeer.models

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class TopicTextCompletionParam(
    @SerializedName("query")
    val query: String,

    @SerializedName("collection_name")
    val collection_name: String? = null,

    @SerializedName("prompt_template")
    val prompt_template: String? = null
) {
    fun toJson(): JsonObject {
        val json = JsonObject()
        json.addProperty("query", query)
        collection_name?.let { json.addProperty("collection_name", it) }
        prompt_template?.let { json.addProperty("prompt_template", it) }
        return json
    }
}
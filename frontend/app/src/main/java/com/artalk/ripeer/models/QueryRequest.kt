package com.artalk.ripeer.models

import com.google.gson.annotations.SerializedName

data class QueryRequest(
    @SerializedName("query")
    val query: String,

    @SerializedName("context")
    val context: String = "Pas de contexte pour cette requête, répond simplement à la question",

    @SerializedName("stream")
    val stream: Boolean = false
)

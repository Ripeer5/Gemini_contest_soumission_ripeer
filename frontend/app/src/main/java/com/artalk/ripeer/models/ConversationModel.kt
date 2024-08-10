package com.artalk.ripeer.models

import java.util.*

data class ConversationModel(
    var id: String = "",
    var userId: String = "",
    var title: String = "",
    var createdAt: Date = Date(),
    var artworkId: String = "",
    var collectionName: String = ""
)

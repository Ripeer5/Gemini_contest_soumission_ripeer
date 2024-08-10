package com.artalk.ripeer.data.fake

import com.artalk.ripeer.models.ConversationModel
import java.util.*

val fakeConversations: List<ConversationModel> = listOf(
    ConversationModel(
        id = "1",
        title = "What's Flutter?",
        createdAt = Date(),
        artworkId = "FlutterArt",
    ),
    ConversationModel(
        id = "2",
        title = "What's Compose?",
        createdAt = Date(),
        artworkId = "ComposeArt",
    ),
    ConversationModel(
        id = "3",
        title = "What's ChatGPT?",
        createdAt = Date(),
        artworkId = "ChatGPTArt",
    ),
)

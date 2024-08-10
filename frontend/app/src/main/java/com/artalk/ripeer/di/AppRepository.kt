package com.artalk.ripeer.di

import com.artalk.ripeer.data.remote.ConversationRepository
import com.artalk.ripeer.data.remote.ConversationRepositoryImpl
import com.artalk.ripeer.data.remote.MessageRepository
import com.artalk.ripeer.data.remote.MessageRepositoryImpl
import com.artalk.ripeer.data.remote.TopicBackendRepository
import com.artalk.ripeer.data.remote.TopicBackendRepositoryImpl
import com.artalk.ripeer.data.repository.TextToSpeechRepository
import com.artalk.ripeer.data.repository.TextToSpeechRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun textToSpeechRepository(
        repo: TextToSpeechRepositoryImpl
    ): TextToSpeechRepository

    @Binds
    abstract fun conversationRepository(
        repo: ConversationRepositoryImpl
    ): ConversationRepository

    @Binds
    abstract fun messageRepository(
        repo: MessageRepositoryImpl
    ): MessageRepository

    @Binds
    abstract fun topicBackendRepository(
        repo: TopicBackendRepositoryImpl
    ): TopicBackendRepository
}

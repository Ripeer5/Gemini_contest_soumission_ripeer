package com.artalk.ripeer.di

import android.content.Context
import com.artalk.ripeer.data.api.ElevenlabsAPI
import com.artalk.ripeer.data.api.TopicBackendApi
import com.artalk.ripeer.constants.baseUrlTestBackend
import com.artalk.ripeer.constants.elevenlabsEndpoint
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideCustomApiService(topicRetrofit: Retrofit): TopicBackendApi =
        topicRetrofit.create(TopicBackendApi::class.java)

    @Singleton
    @Provides
    fun provideTopicOkHttpClient() = OkHttpClient.Builder().build()

    @Singleton
    @Provides
    fun provideTopicRetrofit(customOkHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder()
            .baseUrl(baseUrlTestBackend)
            .client(customOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideElevenlabsAPI(): ElevenlabsAPI {
        val gson = GsonBuilder().setLenient().create()

        val retrofit = Retrofit.Builder()
            .baseUrl(elevenlabsEndpoint)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(ElevenlabsAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}

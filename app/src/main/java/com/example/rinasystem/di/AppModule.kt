package com.example.rinasystem.di

import com.example.rinasystem.BuildConfig
import com.example.rinasystem.data.api.AriaApi
import com.example.rinasystem.data.api.AuthInterceptor
import com.example.rinasystem.data.api.DynamicBaseUrlInterceptor
import com.example.rinasystem.data.local.ServerConfigManager
import com.example.rinasystem.data.local.TokenManager
import com.example.rinasystem.data.ws.AriaWebSocket
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAriaApi(retrofit: Retrofit): AriaApi {
        return retrofit.create(AriaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAriaWebSocket(
        okHttpClient: OkHttpClient,
        tokenManager: TokenManager,
        serverConfig: ServerConfigManager
    ): AriaWebSocket {
        return AriaWebSocket(okHttpClient, tokenManager, serverConfig)
    }
}

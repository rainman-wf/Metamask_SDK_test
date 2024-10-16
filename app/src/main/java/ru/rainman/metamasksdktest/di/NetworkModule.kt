package ru.rainman.metamasksdktest.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import ru.rainman.metamasksdktest.TEST_NET_PROVIDER
import ru.rainman.metamasksdktest.network.EthereumJRPCApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val format = Json { explicitNulls = false }

    @Provides
    @Singleton
    fun provideClient() : OkHttpClient = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }).build()

    @Provides
    @Singleton
    fun provideApi(client: OkHttpClient): EthereumJRPCApi = Retrofit.Builder()
        .baseUrl(TEST_NET_PROVIDER)
        .client(client)
        .addConverterFactory(format.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()
        .create()

}
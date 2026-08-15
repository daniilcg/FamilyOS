package com.familyos.core.data.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import dagger.Provides
import javax.inject.Singleton

/**
 * Provides shared data-layer utilities (JSON and related helpers).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /** Shared kotlinx.serialization [Json] instance. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
}

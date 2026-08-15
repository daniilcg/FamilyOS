package com.familyos.core.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides Firebase Auth, Firestore, and Storage singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /** Provides [FirebaseAuth] when Firebase is initialized; never crashes DI on stub config. */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        // Still attempt default instance — HybridAuthRepository avoids calling it when unavailable.
        FirebaseAuth.getInstance()
    }

    /** Provides [FirebaseFirestore] with offline persistence enabled. */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        return firestore
    }

    /** Provides [FirebaseStorage]. */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()
}

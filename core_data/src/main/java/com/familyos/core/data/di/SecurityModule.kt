package com.familyos.core.data.di

import com.familyos.core.data.security.AesDocumentCipher
import com.familyos.core.data.security.DocumentLockGateImpl
import com.familyos.core.domain.security.DocumentCipher
import com.familyos.core.domain.security.DocumentLockGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds document encryption and vault lock implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindDocumentCipher(impl: AesDocumentCipher): DocumentCipher

    @Binds
    @Singleton
    abstract fun bindDocumentLockGate(impl: DocumentLockGateImpl): DocumentLockGate
}

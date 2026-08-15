package com.familyos.core.data.repository

import com.familyos.core.data.local.dao.UserDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.UserDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Auth + Room offline-first authentication repository.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userDao: UserDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : AuthRepository {

    override fun observeAuthState(): Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
            } else {
                // Prefer Room cache; fall back to Firebase user fields.
                trySend(
                    User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email.orEmpty(),
                        displayName = firebaseUser.displayName.orEmpty(),
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        phoneNumber = firebaseUser.phoneNumber,
                        familyId = null,
                        isEmailVerified = firebaseUser.isEmailVerified,
                        createdAt = 0L,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.map { base ->
        if (base == null) null
        else userDao.getById(base.id)?.toDomain() ?: base
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return userDao.getById(firebaseUser.uid)?.toDomain()
            ?: User(
                id = firebaseUser.uid,
                email = firebaseUser.email.orEmpty(),
                displayName = firebaseUser.displayName.orEmpty(),
                photoUrl = firebaseUser.photoUrl?.toString(),
                phoneNumber = firebaseUser.phoneNumber,
                isEmailVerified = firebaseUser.isEmailVerified,
                updatedAt = System.currentTimeMillis(),
            )
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        Result.runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw AppException(AppError.Unauthorized("Sign-in failed"))
            persistFirebaseUser(firebaseUser.uid, email, firebaseUser.displayName.orEmpty(), firebaseUser.photoUrl?.toString(), firebaseUser.isEmailVerified)
        }

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        Result.runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw AppException(AppError.Unauthorized("Google sign-in failed"))
            persistFirebaseUser(
                firebaseUser.uid,
                firebaseUser.email.orEmpty(),
                firebaseUser.displayName.orEmpty(),
                firebaseUser.photoUrl?.toString(),
                firebaseUser.isEmailVerified,
            )
        }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> =
        Result.runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw AppException(AppError.Remote("Account creation failed"))
            val profile = UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
            firebaseUser.updateProfile(profile).await()
            persistFirebaseUser(firebaseUser.uid, email, displayName, null, false)
        }

    override suspend fun resetPassword(email: String): Result<Unit> =
        Result.runCatching {
            auth.sendPasswordResetEmail(email).await()
        }

    override suspend fun logout(): Result<Unit> =
        Result.runCatching {
            auth.signOut()
        }

    override suspend fun deleteAccount(): Result<Unit> =
        Result.runCatching {
            val user = auth.currentUser ?: throw AppException(AppError.Unauthorized())
            val uid = user.uid
            userDao.delete(uid)
            runCatching { firestoreDataSource.deleteUser(uid) }
            user.delete().await()
        }

    override suspend fun updateProfile(displayName: String?, photoUrl: String?, phoneNumber: String?): Result<User> =
        Result.runCatching {
            val firebaseUser = auth.currentUser ?: throw AppException(AppError.Unauthorized())
            if (displayName != null || photoUrl != null) {
                val builder = UserProfileChangeRequest.Builder()
                if (displayName != null) builder.setDisplayName(displayName)
                if (photoUrl != null) builder.setPhotoUri(android.net.Uri.parse(photoUrl))
                firebaseUser.updateProfile(builder.build()).await()
            }
            val existing = userDao.getById(firebaseUser.uid)?.toDomain()
                ?: throw AppException(AppError.NotFound("User", firebaseUser.uid))
            val updated = existing.copy(
                displayName = displayName ?: existing.displayName,
                photoUrl = photoUrl ?: existing.photoUrl,
                phoneNumber = phoneNumber ?: existing.phoneNumber,
                updatedAt = System.currentTimeMillis(),
            )
            userDao.upsert(updated.toEntity())
            enqueueUser(updated)
            updated
        }

    private suspend fun persistFirebaseUser(
        uid: String,
        email: String,
        displayName: String,
        photoUrl: String?,
        verified: Boolean,
    ): User {
        val now = System.currentTimeMillis()
        val cached = userDao.getById(uid)?.toDomain()
        val user = User(
            id = uid,
            email = email,
            displayName = displayName.ifBlank { cached?.displayName.orEmpty() },
            photoUrl = photoUrl ?: cached?.photoUrl,
            phoneNumber = cached?.phoneNumber,
            familyId = cached?.familyId,
            preferredLanguage = cached?.preferredLanguage ?: "en",
            createdAt = cached?.createdAt?.takeIf { it > 0 } ?: now,
            updatedAt = now,
            isEmailVerified = verified,
        )
        userDao.upsert(user.toEntity())
        enqueueUser(user)
        runCatching {
            firestoreDataSource.upsertUser(
                UserDto(
                    id = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    phoneNumber = user.phoneNumber,
                    familyId = user.familyId,
                    preferredLanguage = user.preferredLanguage,
                    createdAt = user.createdAt,
                    updatedAt = user.updatedAt,
                    isEmailVerified = user.isEmailVerified,
                ),
            )
        }
        return user
    }

    private suspend fun enqueueUser(user: User) {
        syncQueue.enqueue(
            collection = SyncCollection.USERS,
            documentId = user.id,
            familyId = user.familyId,
            actionType = SyncActionType.UPSERT,
            payloadJson = EntityMappers.json.encodeToString(
                UserDto(
                    id = user.id,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    phoneNumber = user.phoneNumber,
                    familyId = user.familyId,
                    preferredLanguage = user.preferredLanguage,
                    createdAt = user.createdAt,
                    updatedAt = user.updatedAt,
                    isEmailVerified = user.isEmailVerified,
                ),
            ),
        )
    }
}

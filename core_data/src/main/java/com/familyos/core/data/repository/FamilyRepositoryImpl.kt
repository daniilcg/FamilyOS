package com.familyos.core.data.repository

import com.familyos.core.data.local.dao.FamilyDao
import com.familyos.core.data.local.dao.UserDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.FamilyDto
import com.familyos.core.data.remote.dto.MemberDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.logic.InviteCodeGenerator
import com.familyos.core.domain.model.Family
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first family and membership repository.
 */
@Singleton
class FamilyRepositoryImpl @Inject constructor(
    private val familyDao: FamilyDao,
    private val userDao: UserDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : FamilyRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeFamily(familyId: String): Flow<Family?> =
        familyDao.observeFamily(familyId).map { it?.toDomain() }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeFamily(familyId).collect { dto ->
                        if (dto != null) familyDao.upsertFamily(dto.toEntity())
                    }
                }
            }
        }

    override fun observeMembers(familyId: String): Flow<List<FamilyMember>> =
        familyDao.observeMembers(familyId).map { list -> list.map { it.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeMembers(familyId).collect { dtos ->
                        dtos.forEach { familyDao.upsertMember(it.toEntity()) }
                    }
                }
            }
        }

    override suspend fun getFamily(familyId: String): Result<Family> = Result.runCatching {
        familyDao.getFamily(familyId)?.toDomain()
            ?: firestoreDataSource.getFamily(familyId)?.toEntity()?.also { familyDao.upsertFamily(it) }?.toDomain()
            ?: throw AppException(AppError.NotFound("Family", familyId))
    }

    override suspend fun createFamily(name: String, ownerId: String): Result<Family> =
        Result.runCatching {
            val now = System.currentTimeMillis()
            val family = Family(
                id = UUID.randomUUID().toString(),
                name = name,
                inviteCode = InviteCodeGenerator.generate(),
                ownerId = ownerId,
                createdAt = now,
                updatedAt = now,
                memberCount = 1,
            )
            familyDao.upsertFamily(family.toEntity())
            val owner = userDao.getById(ownerId)?.toDomain()
            val member = FamilyMember(
                id = UUID.randomUUID().toString(),
                familyId = family.id,
                userId = ownerId,
                displayName = owner?.displayName ?: "Owner",
                photoUrl = owner?.photoUrl,
                email = owner?.email.orEmpty(),
                role = FamilyRole.OWNER,
                joinedAt = now,
                updatedAt = now,
            )
            familyDao.upsertMember(member.toEntity())
            userDao.updateFamilyId(ownerId, family.id, now)
            enqueueFamily(family)
            enqueueMember(member)
            runCatching {
                firestoreDataSource.upsertFamily(family.toDto())
                firestoreDataSource.upsertMember(member.toDto())
            }
            family
        }

    override suspend fun joinFamilyByCode(
        inviteCode: String,
        userId: String,
        displayName: String,
        photoUrl: String?,
        email: String,
    ): Result<Family> = Result.runCatching {
        val local = familyDao.getByInviteCode(inviteCode)?.toDomain()
        val family = local
            ?: firestoreDataSource.findFamilyByInviteCode(inviteCode)?.toEntity()
                ?.also { familyDao.upsertFamily(it) }?.toDomain()
            ?: throw AppException(AppError.NotFound("Family", inviteCode))
        val existing = familyDao.getMemberByUser(family.id, userId)
        if (existing != null) return@runCatching family
        val now = System.currentTimeMillis()
        val member = FamilyMember(
            id = UUID.randomUUID().toString(),
            familyId = family.id,
            userId = userId,
            displayName = displayName,
            photoUrl = photoUrl,
            email = email,
            role = FamilyRole.MEMBER,
            joinedAt = now,
            updatedAt = now,
        )
        familyDao.upsertMember(member.toEntity())
        val count = familyDao.countMembers(family.id)
        familyDao.updateMemberCount(family.id, count, now)
        userDao.updateFamilyId(userId, family.id, now)
        enqueueMember(member)
        runCatching { firestoreDataSource.upsertMember(member.toDto()) }
        family.copy(memberCount = count, updatedAt = now)
    }

    override suspend fun generateInviteCode(familyId: String): Result<String> =
        Result.runCatching {
            val code = InviteCodeGenerator.generate()
            val now = System.currentTimeMillis()
            familyDao.updateInviteCode(familyId, code, now)
            val family = familyDao.getFamily(familyId)?.toDomain()
                ?: throw AppException(AppError.NotFound("Family", familyId))
            enqueueFamily(family.copy(inviteCode = code, updatedAt = now))
            code
        }

    override suspend fun updateMemberRole(familyId: String, memberId: String, role: FamilyRole): Result<FamilyMember> =
        Result.runCatching {
            val now = System.currentTimeMillis()
            familyDao.updateRole(memberId, role.name, now)
            val member = familyDao.getMember(memberId)?.toDomain()
                ?: throw AppException(AppError.NotFound("Member", memberId))
            enqueueMember(member)
            member
        }

    override suspend fun removeMember(familyId: String, memberId: String): Result<Unit> =
        Result.runCatching {
            val member = familyDao.getMember(memberId)?.toDomain()
                ?: throw AppException(AppError.NotFound("Member", memberId))
            if (member.role == FamilyRole.OWNER) {
                throw AppException(AppError.Validation("Cannot remove the family owner"))
            }
            familyDao.deleteMember(memberId)
            val count = familyDao.countMembers(familyId)
            familyDao.updateMemberCount(familyId, count, System.currentTimeMillis())
            syncQueue.enqueue(
                SyncCollection.MEMBERS,
                memberId,
                familyId,
                SyncActionType.DELETE,
                "{}",
            )
            runCatching { firestoreDataSource.deleteMember(familyId, memberId) }
        }

    override suspend fun leaveFamily(familyId: String, userId: String): Result<Unit> =
        Result.runCatching {
            val member = familyDao.getMemberByUser(familyId, userId)
                ?: throw AppException(AppError.NotFound("Member", userId))
            if (member.role == FamilyRole.OWNER.name) {
                throw AppException(AppError.Validation("Owner must transfer ownership before leaving"))
            }
            removeMember(familyId, member.id).getOrNull()
                ?: throw AppException(AppError.Unknown("Failed to leave family"))
            userDao.updateFamilyId(userId, null, System.currentTimeMillis())
        }

    override suspend fun updateFamily(family: Family): Result<Family> =
        Result.runCatching {
            val updated = family.copy(updatedAt = System.currentTimeMillis())
            familyDao.upsertFamily(updated.toEntity())
            enqueueFamily(updated)
            updated
        }

    private suspend fun enqueueFamily(family: Family) {
        syncQueue.enqueue(
            SyncCollection.FAMILIES,
            family.id,
            family.id,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(family.toDto()),
        )
    }

    private suspend fun enqueueMember(member: FamilyMember) {
        syncQueue.enqueue(
            SyncCollection.MEMBERS,
            member.id,
            member.familyId,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(member.toDto()),
        )
    }

    private fun Family.toDto() = FamilyDto(
        id, name, inviteCode, ownerId, photoUrl, createdAt, updatedAt, memberCount,
    )

    private fun FamilyDto.toEntity() = com.familyos.core.data.local.entity.FamilyEntity(
        id, name, inviteCode, ownerId, photoUrl, createdAt, updatedAt, memberCount,
    )

    private fun FamilyMember.toDto() = MemberDto(
        id, familyId, userId, displayName, photoUrl, email, role.name, joinedAt, updatedAt,
    )

    private fun MemberDto.toEntity() = com.familyos.core.data.local.entity.MemberEntity(
        id, familyId, userId, displayName, photoUrl, email, role, joinedAt, updatedAt,
    )
}

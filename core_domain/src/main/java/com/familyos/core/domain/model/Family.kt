package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * A family workspace that groups members and shared data.
 *
 * @property id Unique family id
 * @property name Display name
 * @property inviteCode Current join code
 * @property ownerId User id of the OWNER
 * @property photoUrl Optional family avatar
 * @property createdAt Creation timestamp
 * @property updatedAt Last mutation timestamp
 * @property memberCount Cached member count
 */
@Serializable
data class Family(
    val id: String,
    val name: String,
    val inviteCode: String,
    val ownerId: String,
    val photoUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val memberCount: Int = 1,
)

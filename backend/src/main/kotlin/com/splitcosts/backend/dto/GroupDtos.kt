package com.splitcosts.backend.dto

data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

data class GroupDto(
    val id: Long,
    val name: String,
    val description: String?,
    val admin: UserDto,
    val members: List<UserDto>
)

data class AddMemberRequest(
    val usernameOrEmail: String
)

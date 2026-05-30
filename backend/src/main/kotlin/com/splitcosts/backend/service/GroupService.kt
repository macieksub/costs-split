package com.splitcosts.backend.service

import com.splitcosts.backend.dto.AddMemberRequest
import com.splitcosts.backend.dto.CreateGroupRequest
import com.splitcosts.backend.dto.GroupDto
import com.splitcosts.backend.dto.toDto
import com.splitcosts.backend.model.Group
import com.splitcosts.backend.model.User
import com.splitcosts.backend.repository.GroupRepository
import com.splitcosts.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createGroup(request: CreateGroupRequest, admin: User): GroupDto {
        val group = Group(
            name = request.name,
            description = request.description,
            admin = admin,
            members = mutableSetOf(admin) // The admin is automatically the first member
        )
        return groupRepository.save(group).toDto()
    }

    @Transactional(readOnly = true)
    fun getGroupsForUser(user: User): List<GroupDto> {
        return groupRepository.findByUser(user).map { g ->
            // In a real application, make sure members are loaded cleanly
            g.toDto()
        }
    }

    @Transactional(readOnly = true)
    fun getGroupById(groupId: Long, user: User): GroupDto {
        val group = getGroupEntity(groupId)
        verifyMember(group, user)
        return group.toDto()
    }

    @Transactional
    fun addMember(groupId: Long, request: AddMemberRequest, currentUser: User): GroupDto {
        val group = getGroupEntity(groupId)
        verifyAdmin(group, currentUser)

        val newMember = userRepository.findByUsername(request.usernameOrEmail)
            .or { userRepository.findByEmail(request.usernameOrEmail) }
            .orElseThrow { IllegalArgumentException("User '${request.usernameOrEmail}' not found.") }

        if (group.members.contains(newMember)) {
            throw IllegalArgumentException("User '${newMember.name}' is already a member of this group.")
        }

        group.members.add(newMember)
        return groupRepository.save(group).toDto()
    }

    @Transactional
    fun removeMember(groupId: Long, memberUserId: Long, currentUser: User): GroupDto {
        val group = getGroupEntity(groupId)
        verifyAdmin(group, currentUser)

        if (group.admin.id == memberUserId) {
            throw IllegalArgumentException("Cannot remove the administrator from the group.")
        }

        val memberToRemove = userRepository.findById(memberUserId)
            .orElseThrow { IllegalArgumentException("User with id $memberUserId not found.") }

        if (!group.members.contains(memberToRemove)) {
            throw IllegalArgumentException("User is not a member of this group.")
        }

        group.members.remove(memberToRemove)
        return groupRepository.save(group).toDto()
    }

    @Transactional
    fun deleteGroup(groupId: Long, currentUser: User) {
        val group = getGroupEntity(groupId)
        verifyAdmin(group, currentUser)
        groupRepository.delete(group)
    }

    @Transactional(readOnly = true)
    fun getGroupEntity(groupId: Long): Group {
        return groupRepository.findById(groupId)
            .orElseThrow { NoSuchElementException("Group with id $groupId not found.") }
    }

    fun verifyMember(group: Group, user: User) {
        if (group.admin.id != user.id && group.members.none { it.id == user.id }) {
            throw SecurityException("You are not a member of this group.")
        }
    }

    fun verifyAdmin(group: Group, user: User) {
        if (group.admin.id != user.id) {
            throw SecurityException("Only the group administrator can perform this action.")
        }
    }
}

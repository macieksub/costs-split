package com.splitcosts.backend.controller

import com.splitcosts.backend.dto.AddMemberRequest
import com.splitcosts.backend.dto.CreateGroupRequest
import com.splitcosts.backend.dto.GroupDto
import com.splitcosts.backend.security.UserPrincipal
import com.splitcosts.backend.service.GroupService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups")
class GroupController(private val groupService: GroupService) {

    @PostMapping
    fun createGroup(
        @RequestBody request: CreateGroupRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<GroupDto> {
        val group = groupService.createGroup(request, principal.user)
        return ResponseEntity.ok(group)
    }

    @GetMapping
    fun getGroups(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<List<GroupDto>> {
        val groups = groupService.getGroupsForUser(principal.user)
        return ResponseEntity.ok(groups)
    }

    @GetMapping("/{id}")
    fun getGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<GroupDto> {
        val group = groupService.getGroupById(id, principal.user)
        return ResponseEntity.ok(group)
    }

    @PostMapping("/{id}/members")
    fun addMember(
        @PathVariable id: Long,
        @RequestBody request: AddMemberRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<GroupDto> {
        val group = groupService.addMember(id, request, principal.user)
        return ResponseEntity.ok(group)
    }

    @DeleteMapping("/{id}/members/{userId}")
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<GroupDto> {
        val group = groupService.removeMember(id, userId, principal.user)
        return ResponseEntity.ok(group)
    }

    @DeleteMapping("/{id}")
    fun deleteGroup(
        @PathVariable id: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        groupService.deleteGroup(id, principal.user)
        return ResponseEntity.noContent().build()
    }
}

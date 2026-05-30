package com.splitcosts.backend.controller

import com.splitcosts.backend.dto.AuthResponse
import com.splitcosts.backend.dto.LoginRequest
import com.splitcosts.backend.dto.RegisterRequest
import com.splitcosts.backend.dto.UserDto
import com.splitcosts.backend.dto.toDto
import com.splitcosts.backend.security.UserPrincipal
import com.splitcosts.backend.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = authService.login(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/me")
    fun getProfile(@AuthenticationPrincipal principal: UserPrincipal?): ResponseEntity<UserDto> {
        if (principal == null) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.ok(principal.user.toDto())
    }
}

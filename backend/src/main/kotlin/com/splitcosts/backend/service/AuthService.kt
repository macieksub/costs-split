package com.splitcosts.backend.service

import com.splitcosts.backend.dto.AuthResponse
import com.splitcosts.backend.dto.LoginRequest
import com.splitcosts.backend.dto.RegisterRequest
import com.splitcosts.backend.dto.toDto
import com.splitcosts.backend.model.User
import com.splitcosts.backend.repository.UserRepository
import com.splitcosts.backend.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username '${request.username}' is already taken.")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email '${request.email}' is already in use.")
        }

        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)!!,
            name = request.name
        )
        
        val savedUser = userRepository.save(user)
        
        // Log in the newly registered user automatically
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )
        SecurityContextHolder.getContext().authentication = authentication
        
        val token = jwtService.generateToken(authentication.principal as org.springframework.security.core.userdetails.UserDetails)
        
        return AuthResponse(token, savedUser.toDto())
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.usernameOrEmail, request.password)
        )
        SecurityContextHolder.getContext().authentication = authentication

        val userPrincipal = authentication.principal as com.splitcosts.backend.security.UserPrincipal
        val token = jwtService.generateToken(userPrincipal)

        return AuthResponse(token, userPrincipal.user.toDto())
    }
}

package com.splitcosts.backend.security

import com.splitcosts.backend.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    @Transactional(readOnly = true)
    override fun loadUserByUsername(usernameOrEmail: String): UserDetails {
        val user = userRepository.findByUsername(usernameOrEmail)
            .or { userRepository.findByEmail(usernameOrEmail) }
            .orElseThrow {
                UsernameNotFoundException("User not found with username or email: $usernameOrEmail")
            }
        return UserPrincipal(user)
    }

    @Transactional(readOnly = true)
    fun loadUserById(id: Long): UserDetails {
        val user = userRepository.findById(id).orElseThrow {
            UsernameNotFoundException("User not found with id: $id")
        }
        return UserPrincipal(user)
    }
}

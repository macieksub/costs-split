package org.example.frontend.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.frontend.model.*

object ApiClient {
    private const val BASE_URL = "http://localhost:8080"
    
    var token: String? = null
    var currentUser: UserDto? = null

    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private fun HttpRequestBuilder.authHeader() {
        token?.let {
            headers.append(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    // AUTH
    suspend fun register(request: RegisterRequest): AuthResponse {
        val response: AuthResponse = client.post("$BASE_URL/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        token = response.token
        currentUser = response.user
        
        // Persist session
        org.example.frontend.util.saveLocalStorage("auth_token", response.token)
        val userJson = Json.encodeToString(UserDto.serializer(), response.user)
        org.example.frontend.util.saveLocalStorage("auth_user", userJson)
        
        return response
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val response: AuthResponse = client.post("$BASE_URL/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
        token = response.token
        currentUser = response.user
        
        // Persist session
        org.example.frontend.util.saveLocalStorage("auth_token", response.token)
        val userJson = Json.encodeToString(UserDto.serializer(), response.user)
        org.example.frontend.util.saveLocalStorage("auth_user", userJson)
        
        return response
    }

    suspend fun getProfile(): UserDto {
        val response: UserDto = client.get("$BASE_URL/api/auth/me") {
            authHeader()
        }.body()
        currentUser = response
        return response
    }

    fun logout() {
        token = null
        currentUser = null
        org.example.frontend.util.removeLocalStorage("auth_token")
        org.example.frontend.util.removeLocalStorage("auth_user")
    }

    fun loadSession(): Boolean {
        val savedToken = org.example.frontend.util.loadLocalStorage("auth_token")
        val savedUserJson = org.example.frontend.util.loadLocalStorage("auth_user")
        if (!savedToken.isNullOrBlank() && savedToken != "null" && !savedUserJson.isNullOrBlank() && savedUserJson != "null") {
            try {
                token = savedToken
                currentUser = Json.decodeFromString(UserDto.serializer(), savedUserJson)
                return true
            } catch (e: Exception) {
                logout()
            }
        }
        return false
    }

    // GROUPS
    suspend fun getGroups(): List<GroupDto> {
        return client.get("$BASE_URL/api/groups") {
            authHeader()
        }.body()
    }

    suspend fun createGroup(request: CreateGroupRequest): GroupDto {
        return client.post("$BASE_URL/api/groups") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getGroupDetails(groupId: Long): GroupDto {
        return client.get("$BASE_URL/api/groups/$groupId") {
            authHeader()
        }.body()
    }

    suspend fun addMember(groupId: Long, request: AddMemberRequest): GroupDto {
        return client.post("$BASE_URL/api/groups/$groupId/members") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun removeMember(groupId: Long, userId: Long): GroupDto {
        return client.delete("$BASE_URL/api/groups/$groupId/members/$userId") {
            authHeader()
        }.body()
    }

    suspend fun deleteGroup(groupId: Long) {
        client.delete("$BASE_URL/api/groups/$groupId") {
            authHeader()
        }
    }

    // EXPENSES
    suspend fun getExpenses(groupId: Long): List<ExpenseDto> {
        return client.get("$BASE_URL/api/groups/$groupId/expenses") {
            authHeader()
        }.body()
    }

    suspend fun createExpense(groupId: Long, request: CreateExpenseRequest): ExpenseDto {
        return client.post("$BASE_URL/api/groups/$groupId/expenses") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun deleteExpense(groupId: Long, expenseId: Long) {
        client.delete("$BASE_URL/api/groups/$groupId/expenses/$expenseId") {
            authHeader()
        }
    }

    // BALANCES & SETTLEMENTS
    suspend fun getBalances(groupId: Long): GroupBalancesDto {
        return client.get("$BASE_URL/api/groups/$groupId/balances") {
            authHeader()
        }.body()
    }

    suspend fun getSettlements(groupId: Long): List<SettlementDto> {
        return client.get("$BASE_URL/api/groups/$groupId/settlements") {
            authHeader()
        }.body()
    }

    suspend fun createSettlement(groupId: Long, request: CreateSettlementRequest): SettlementDto {
        return client.post("$BASE_URL/api/groups/$groupId/settlements") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun approveSettlement(groupId: Long, settlementId: Long): SettlementDto {
        return client.post("$BASE_URL/api/groups/$groupId/settlements/$settlementId/approve") {
            authHeader()
        }.body()
    }
}

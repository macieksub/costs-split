package org.example.frontend.ui

import androidx.compose.runtime.*
import org.example.frontend.api.ApiClient
import org.example.frontend.model.UserDto

sealed class Screen {
    object Login : Screen()
    object Register : Screen()
    object GroupList : Screen()
    data class GroupDetails(val groupId: Long) : Screen()
    data class AddExpense(val groupId: Long) : Screen()
    data class BalancesAndSettlements(val groupId: Long) : Screen()
}

object AppState {
    val backStack = mutableStateListOf<Screen>(Screen.Login)
    
    var currentUser by mutableStateOf<UserDto?>(null)
    var currentError by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun replaceWith(screen: Screen) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
        backStack.add(screen)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun showError(message: String) {
        currentError = message
        successMessage = null
    }

    fun showSuccess(message: String) {
        successMessage = message
        currentError = null
    }

    fun clearMessages() {
        currentError = null
        successMessage = null
    }

    fun logout() {
        ApiClient.logout()
        currentUser = null
        backStack.clear()
        backStack.add(Screen.Login)
    }
}

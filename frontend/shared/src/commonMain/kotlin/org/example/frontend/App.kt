package org.example.frontend

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import org.example.frontend.ui.AppState
import org.example.frontend.ui.Screen
import org.example.frontend.ui.screens.*

@Composable
@Preview
fun App() {
    // Router based on AppState backstack
    val currentScreen = AppState.backStack.lastOrNull() ?: Screen.Login

    when (currentScreen) {
        is Screen.Login -> LoginScreen()
        is Screen.Register -> RegisterScreen()
        is Screen.GroupList -> GroupListScreen()
        is Screen.GroupDetails -> GroupDetailsScreen(groupId = currentScreen.groupId)
        is Screen.AddExpense -> AddExpenseScreen(groupId = currentScreen.groupId)
        is Screen.BalancesAndSettlements -> BalancesScreen(groupId = currentScreen.groupId)
    }
}
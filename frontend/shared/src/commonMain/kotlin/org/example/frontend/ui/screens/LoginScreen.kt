package org.example.frontend.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.frontend.api.ApiClient
import org.example.frontend.model.LoginRequest
import org.example.frontend.ui.*

@Composable
fun LoginScreen() {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    BaseScreenLayout {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Title
            Text(
                text = "Costs Split",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Podziel koszty w grupie w kilka kliknięć",
                fontSize = 15.sp,
                color = PremiumTheme.TextSecondary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Glassmorphic Login Form
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 450.dp)
            ) {
                Text(
                    text = "Zaloguj się",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                PremiumTextField(
                    value = usernameOrEmail,
                    onValueChange = { usernameOrEmail = it },
                    label = "Nazwa użytkownika lub Email",
                    placeholder = "Wpisz swój login...",
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                PremiumTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Hasło",
                    placeholder = "Wpisz swoje hasło...",
                    isPassword = true,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                GlowButton(
                    text = "Zaloguj się",
                    onClick = {
                        if (usernameOrEmail.isBlank() || password.isBlank()) {
                            AppState.showError("Wypełnij wszystkie pola!")
                            return@GlowButton
                        }
                        
                        scope.launch {
                            AppState.isLoading = true
                            AppState.clearMessages()
                            try {
                                val response = ApiClient.login(LoginRequest(usernameOrEmail.trim(), password))
                                AppState.currentUser = response.user
                                AppState.showSuccess("Witaj z powrotem, ${response.user.name}!")
                                AppState.replaceWith(Screen.GroupList)
                            } catch (e: Exception) {
                                AppState.showError("Błąd logowania: Błędne dane użytkownika lub hasło.")
                            } finally {
                                AppState.isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Nie masz konta? ", color = PremiumTheme.TextSecondary, fontSize = 14.sp)
                    Text(
                        text = "Zarejestruj się",
                        color = PremiumTheme.NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                AppState.clearMessages()
                                AppState.navigateTo(Screen.Register)
                            }
                    )
                }
            }
        }
    }
}

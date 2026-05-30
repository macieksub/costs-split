package org.example.frontend.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import org.example.frontend.model.RegisterRequest
import org.example.frontend.ui.*

@Composable
fun RegisterScreen() {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    BaseScreenLayout {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Załóż Konto",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 450.dp)
            ) {
                Text(
                    text = "Zarejestruj się",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Imię i Nazwisko",
                    placeholder = "Np. Jan Kowalski",
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PremiumTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Nazwa użytkownika",
                    placeholder = "Np. janek",
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PremiumTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "Np. jan@example.com",
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PremiumTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Hasło",
                    placeholder = "Wpisz bezpieczne hasło...",
                    isPassword = true,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                GlowButton(
                    text = "Zarejestruj się",
                    onClick = {
                        if (name.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
                            AppState.showError("Wypełnij wszystkie pola!")
                            return@GlowButton
                        }
                        if (!email.contains("@")) {
                            AppState.showError("Podaj poprawny adres email!")
                            return@GlowButton
                        }
                        if (password.length < 6) {
                            AppState.showError("Hasło musi mieć co najmniej 6 znaków!")
                            return@GlowButton
                        }

                        scope.launch {
                            AppState.isLoading = true
                            AppState.clearMessages()
                            try {
                                val response = ApiClient.register(
                                    RegisterRequest(
                                        username = username.trim(),
                                        email = email.trim(),
                                        password = password,
                                        name = name.trim()
                                    )
                                )
                                AppState.currentUser = response.user
                                AppState.showSuccess("Konto zarejestrowane pomyślnie. Witaj, ${response.user.name}!")
                                AppState.replaceWith(Screen.GroupList)
                            } catch (e: Exception) {
                                AppState.showError("Rejestracja nie powiodła się. Nazwa użytkownika lub email mogą być zajęte.")
                            } finally {
                                AppState.isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = PremiumTheme.SoftPink
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Masz już konto? ", color = PremiumTheme.TextSecondary, fontSize = 14.sp)
                    Text(
                        text = "Zaloguj się",
                        color = PremiumTheme.NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                AppState.clearMessages()
                                AppState.goBack()
                            }
                    )
                }
            }
        }
    }
}

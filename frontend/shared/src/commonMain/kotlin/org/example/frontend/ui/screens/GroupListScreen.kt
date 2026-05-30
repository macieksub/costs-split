package org.example.frontend.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.frontend.api.ApiClient
import org.example.frontend.model.CreateGroupRequest
import org.example.frontend.model.GroupDto
import org.example.frontend.ui.*

@Composable
fun GroupListScreen() {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDescription by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()

    fun loadGroups() {
        scope.launch {
            AppState.isLoading = true
            try {
                groups = ApiClient.getGroups()
            } catch (e: Exception) {
                AppState.showError("Nie udało się pobrać listy grup.")
            } finally {
                AppState.isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadGroups()
    }

    BaseScreenLayout(
        title = "Twoje Grupy",
        actionContent = {
            Text(
                text = AppState.currentUser?.name ?: "",
                color = PremiumTheme.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x33EF4444))
                    .clickable { AppState.logout() }
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text("Wyloguj", color = PremiumTheme.CoralRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Create New Group
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Utwórz Nową Grupę",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    PremiumTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = "Nazwa grupy",
                        placeholder = "Np. Wyjazd w Bieszczady",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    PremiumTextField(
                        value = newGroupDescription,
                        onValueChange = { newGroupDescription = it },
                        label = "Opis grupy",
                        placeholder = "Np. Podział wspólnych kosztów...",
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    GlowButton(
                        text = "Stwórz Grupę",
                        onClick = {
                            if (newGroupName.isBlank()) {
                                AppState.showError("Podaj nazwę grupy!")
                                return@GlowButton
                            }
                            
                            scope.launch {
                                AppState.isLoading = true
                                AppState.clearMessages()
                                try {
                                    ApiClient.createGroup(
                                        CreateGroupRequest(
                                            name = newGroupName.trim(),
                                            description = newGroupDescription.trim().ifBlank { null }
                                        )
                                    )
                                    newGroupName = ""
                                    newGroupDescription = ""
                                    AppState.showSuccess("Grupa została pomyślnie utworzona!")
                                    // Reload list
                                    groups = ApiClient.getGroups()
                                } catch (e: Exception) {
                                    AppState.showError("Nie udało się utworzyć grupy.")
                                } finally {
                                    AppState.isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Right Column: Groups List
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Aktywne Grupy (${groups.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (groups.isEmpty() && !AppState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PremiumTheme.GlassCardBg, PremiumTheme.CardShape)
                            .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nie należysz jeszcze do żadnej grupy.\nStwórz nową obok, aby zacząć!",
                            color = PremiumTheme.TextSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(groups) { group ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PremiumTheme.CardShape)
                                    .background(PremiumTheme.GlassCardBg)
                                    .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape)
                                    .clickable { AppState.navigateTo(Screen.GroupDetails(group.id)) }
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = group.name,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (!group.description.isNullOrBlank()) {
                                            Text(
                                                text = group.description,
                                                color = PremiumTheme.TextSecondary,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Członkowie: ${group.members.size}",
                                            color = PremiumTheme.TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Role indicator
                                    val isAdmin = group.admin.id == AppState.currentUser?.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isAdmin) Color(0x3306B6D4) else Color(0x1F293548))
                                            .padding(vertical = 6.dp, horizontal = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isAdmin) "Admin" else "Członek",
                                            color = if (isAdmin) PremiumTheme.NeonCyan else PremiumTheme.TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
import org.example.frontend.model.AddMemberRequest
import org.example.frontend.model.ExpenseDto
import org.example.frontend.model.GroupDto
import org.example.frontend.ui.*

@Composable
fun GroupDetailsScreen(groupId: Long) {
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var expenses by remember { mutableStateOf<List<ExpenseDto>>(emptyList()) }
    var memberInput by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    fun loadData() {
        scope.launch {
            AppState.isLoading = true
            try {
                group = ApiClient.getGroupDetails(groupId)
                expenses = ApiClient.getExpenses(groupId)
            } catch (e: Exception) {
                AppState.showError("Nie udało się załadować danych grupy.")
            } finally {
                AppState.isLoading = false
            }
        }
    }

    LaunchedEffect(groupId) {
        loadData()
    }

    val currentGroup = group

    if (currentGroup == null) {
        BaseScreenLayout(title = "Ładowanie...", showBackButton = true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumTheme.NeonCyan)
            }
        }
        return
    }

    val isAdmin = currentGroup.admin.id == AppState.currentUser?.id

    BaseScreenLayout(
        title = currentGroup.name,
        showBackButton = true,
        actionContent = {
            // Group Navigation Shortcuts
            GlowButton(
                text = "Bilanse i Spłaty",
                onClick = { AppState.navigateTo(Screen.BalancesAndSettlements(groupId)) },
                color = PremiumTheme.AmethystPurple,
                modifier = Modifier.padding(end = 12.dp)
            )
            GlowButton(
                text = "+ Dodaj Wydatek",
                onClick = { AppState.navigateTo(Screen.AddExpense(groupId)) },
                color = PremiumTheme.EmeraldGreen
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Members & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Członkowie grupy (${currentGroup.members.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Members List Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    if (isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PremiumTextField(
                                value = memberInput,
                                onValueChange = { memberInput = it },
                                label = "Zaproś użytkownika",
                                placeholder = "Nazwa lub email...",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .padding(top = 22.dp)
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PremiumTheme.NeonCyan)
                                    .clickable {
                                        if (memberInput.isBlank()) return@clickable
                                        scope.launch {
                                            AppState.isLoading = true
                                            AppState.clearMessages()
                                            try {
                                                group = ApiClient.addMember(groupId, AddMemberRequest(memberInput.trim()))
                                                memberInput = ""
                                                AppState.showSuccess("Dodano użytkownika!")
                                            } catch (e: Exception) {
                                                AppState.showError("Nie udało się dodać użytkownika. Sprawdź poprawność loginu/emaila.")
                                            } finally {
                                                AppState.isLoading = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentGroup.members) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x0AFFFFFF))
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(member.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("@${member.username}", color = PremiumTheme.TextSecondary, fontSize = 12.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isUserAdmin = currentGroup.admin.id == member.id
                                    if (isUserAdmin) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x2206B6D4))
                                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                        ) {
                                            Text("Admin", color = PremiumTheme.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (isAdmin) {
                                        // Admin can kick members
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x22EF4444))
                                                .clickable {
                                                    scope.launch {
                                                        AppState.isLoading = true
                                                        AppState.clearMessages()
                                                        try {
                                                            group = ApiClient.removeMember(groupId, member.id)
                                                            expenses = ApiClient.getExpenses(groupId) // Reload expenses in case split member is removed
                                                            AppState.showSuccess("Usunięto użytkownika ${member.name} z grupy.")
                                                        } catch (e: Exception) {
                                                            AppState.showError("Nie udało się usunąć użytkownika.")
                                                        } finally {
                                                            AppState.isLoading = false
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                        ) {
                                            Text("Usuń", color = PremiumTheme.CoralRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Delete group if Admin
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, PremiumTheme.CoralRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color(0x11EF4444))
                            .clickable {
                                scope.launch {
                                    AppState.isLoading = true
                                    AppState.clearMessages()
                                    try {
                                        ApiClient.deleteGroup(groupId)
                                        AppState.showSuccess("Grupa została pomyślnie usunięta.")
                                        AppState.replaceWith(Screen.GroupList)
                                    } catch (e: Exception) {
                                        AppState.showError("Błąd podczas usuwania grupy.")
                                    } finally {
                                        AppState.isLoading = false
                                    }
                                }
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Usuń tę Grupę", color = PremiumTheme.CoralRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Right Column: Expenses List
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Historia wydatków (${expenses.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (expenses.isEmpty() && !AppState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PremiumTheme.GlassCardBg, PremiumTheme.CardShape)
                            .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Brak zarejestrowanych wydatków.\nKliknij \"+ Dodaj Wydatek\", aby dodać pierwszy koszt!",
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
                        items(expenses) { expense ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PremiumTheme.CardShape)
                                    .background(PremiumTheme.GlassCardBg)
                                    .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape)
                                    .padding(18.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = expense.description,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Zapłacone przez: ${expense.paidBy.name}",
                                                color = PremiumTheme.TextSecondary,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${expense.amount} ${expense.currency}",
                                                color = PremiumTheme.NeonCyan,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            
                                            // Show Delete icon if Admin or Payer
                                            val isPayer = expense.paidBy.id == AppState.currentUser?.id
                                            if (isAdmin || isPayer) {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x33EF4444))
                                                        .clickable {
                                                            scope.launch {
                                                                AppState.isLoading = true
                                                                AppState.clearMessages()
                                                                try {
                                                                    ApiClient.deleteExpense(groupId, expense.id)
                                                                    expenses = ApiClient.getExpenses(groupId)
                                                                    AppState.showSuccess("Wydatek został usunięty.")
                                                                } catch (e: Exception) {
                                                                    AppState.showError("Nie udało się usunąć wydatku.")
                                                                } finally {
                                                                    AppState.isLoading = false
                                                                }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("X", color = PremiumTheme.CoralRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    // Expandable/List of splits details
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x06FFFFFF))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Podział kosztów:",
                                                color = PremiumTheme.TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            expense.splits.forEach { split ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(split.user.name, color = PremiumTheme.TextSecondary, fontSize = 12.sp)
                                                    Text("${split.amount} ${expense.currency}", color = Color.White, fontSize = 12.sp)
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
        }
    }
}

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
import org.example.frontend.model.CreateExpenseRequest
import org.example.frontend.model.CreateSplitRequest
import org.example.frontend.model.GroupDto
import org.example.frontend.model.UserDto
import org.example.frontend.ui.*

@Composable
fun AddExpenseScreen(groupId: Long) {
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PLN") }
    var paidByUserId by remember { mutableStateOf<Long?>(null) }
    
    // Custom splits state
    var isCustomSplit by remember { mutableStateOf(false) }
    // Map member ID to custom split amount string
    val customSplits = remember { mutableStateMapOf<Long, String>() }

    val scope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        scope.launch {
            AppState.isLoading = true
            try {
                val g = ApiClient.getGroupDetails(groupId)
                group = g
                // Default paidBy to current logged-in user if they are in the group
                val current = AppState.currentUser
                if (current != null && g.members.any { it.id == current.id }) {
                    paidByUserId = current.id
                } else if (g.members.isNotEmpty()) {
                    paidByUserId = g.members.first().id
                }
            } catch (e: Exception) {
                AppState.showError("Nie udało się pobrać członków grupy.")
            } finally {
                AppState.isLoading = false
            }
        }
    }

    val currentGroup = group

    if (currentGroup == null) {
        BaseScreenLayout(title = "Dodaj Wydatek", showBackButton = true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumTheme.NeonCyan)
            }
        }
        return
    }

    // Currencies list
    val currencies = listOf("PLN", "EUR", "USD", "GBP")

    BaseScreenLayout(
        title = "Dodaj Wydatek do grupy",
        showBackButton = true
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Main Expense Data
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Szczegóły wydatku",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    PremiumTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Opis wydatku",
                        placeholder = "Np. Zakupy spożywcze, Paliwo...",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = "Kwota",
                            placeholder = "0.00",
                            modifier = Modifier.weight(1.5f)
                        )

                        // Currency Selector Box
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Waluta",
                                color = PremiumTheme.TextSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0AFFFFFF))
                                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                currencies.forEach { curr ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(0.8f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (currency == curr) PremiumTheme.NeonCyan else Color.Transparent)
                                            .clickable { currency = curr }
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = curr,
                                            color = if (currency == curr) Color.White else PremiumTheme.TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Payer dropdown selection list (Simple implementation)
                    Text(
                        text = "Kto zapłacił?",
                        color = PremiumTheme.TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .clickable {
                                // Rotate Payer for ease
                                val index = currentGroup.members.indexOfFirst { it.id == paidByUserId }
                                val nextIndex = (index + 1) % currentGroup.members.size
                                paidByUserId = currentGroup.members[nextIndex].id
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val activePayer = currentGroup.members.find { it.id == paidByUserId }
                        Text(
                            text = activePayer?.name ?: "Wybierz osobę...",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "(Kliknij by zmienić)",
                            color = PremiumTheme.NeonCyan,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GlowButton(
                        text = "Dodaj Wydatek",
                        onClick = {
                            val totalAmount = amountStr.toDoubleOrNull()
                            if (description.isBlank() || totalAmount == null || totalAmount <= 0) {
                                AppState.showError("Podaj poprawny opis oraz kwotę dodatnią!")
                                return@GlowButton
                            }
                            val activePayerId = paidByUserId
                            if (activePayerId == null) {
                                AppState.showError("Wybierz osobę płacącą!")
                                return@GlowButton
                            }

                            // Build splits list if custom
                            val splitsList = mutableListOf<CreateSplitRequest>()
                            if (isCustomSplit) {
                                var sum = 0.0
                                for (member in currentGroup.members) {
                                    val memberAmount = customSplits[member.id]?.toDoubleOrNull() ?: 0.0
                                    if (memberAmount < 0) {
                                        AppState.showError("Kwoty podziału muszą być nieujemne!")
                                        return@GlowButton
                                    }
                                    if (memberAmount > 0) {
                                        splitsList.add(CreateSplitRequest(member.id, memberAmount))
                                        sum += memberAmount
                                    }
                                }

                                // Compare sum with total amount (allow tiny margin of error)
                                if (kotlin.math.abs(sum - totalAmount) > 0.01) {
                                    AppState.showError("Suma podziału ($sum) różni się od kwoty wydatku ($totalAmount)!")
                                    return@GlowButton
                                }
                            }

                            scope.launch {
                                AppState.isLoading = true
                                AppState.clearMessages()
                                try {
                                    ApiClient.createExpense(
                                        groupId = groupId,
                                        request = CreateExpenseRequest(
                                            description = description.trim(),
                                            amount = totalAmount,
                                            currency = currency,
                                            paidByUserId = activePayerId,
                                            splits = splitsList
                                        )
                                    )
                                    AppState.showSuccess("Pomyślnie dodano wydatek!")
                                    AppState.goBack()
                                } catch (e: Exception) {
                                    AppState.showError("Błąd serwera przy dodawaniu wydatku.")
                                } finally {
                                    AppState.isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = PremiumTheme.EmeraldGreen
                    )
                }
            }

            // Right Column: Splitting Controls
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Sposób podziału kosztów",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                GlassCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Split toggles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x0AFFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isCustomSplit) PremiumTheme.NeonCyan else Color.Transparent)
                                .clickable { isCustomSplit = false }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Równy Podział",
                                color = if (!isCustomSplit) Color.White else PremiumTheme.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCustomSplit) PremiumTheme.NeonCyan else Color.Transparent)
                                .clickable { isCustomSplit = true }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Niestandardowy Podział",
                                color = if (isCustomSplit) Color.White else PremiumTheme.TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!isCustomSplit) {
                        // Equal split guide
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Kwota zostanie podzielona po równo\npomiędzy wszystkich ${currentGroup.members.size} członków grupy.\n\nAutomatyczne zaokrąglenie w groszach.",
                                color = PremiumTheme.TextSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        // Custom splits breakdown
                        var totalCustomSum by remember { mutableStateOf(0.0) }
                        
                        fun recalculateSum() {
                            totalCustomSum = currentGroup.members.sumOf {
                                customSplits[it.id]?.toDoubleOrNull() ?: 0.0
                            }
                        }

                        // Validation pill info
                        val expenseAmount = amountStr.toDoubleOrNull() ?: 0.0
                        val isMatched = kotlin.math.abs(totalCustomSum - expenseAmount) < 0.01 && expenseAmount > 0.0
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ręczny podział kwot:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMatched) Color(0x3310B981) else Color(0x33EF4444))
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "$totalCustomSum / $expenseAmount $currency",
                                    color = if (isMatched) PremiumTheme.EmeraldGreen else PremiumTheme.CoralRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(currentGroup.members) { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = member.name,
                                        color = PremiumTheme.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    // Custom input box for amount
                                    val currentVal = customSplits[member.id] ?: ""
                                    OutlinedTextField(
                                        value = currentVal,
                                        onValueChange = {
                                            customSplits[member.id] = it
                                            recalculateSum()
                                        },
                                        placeholder = { Text("0.00", color = Color(0x33FFFFFF)) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = PremiumTheme.NeonCyan,
                                            unfocusedBorderColor = Color(0x11FFFFFF)
                                        ),
                                        modifier = Modifier.width(100.dp).height(48.dp)
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

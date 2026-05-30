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
import org.example.frontend.model.*
import org.example.frontend.ui.*

@Composable
fun BalancesScreen(groupId: Long) {
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var balancesDto by remember { mutableStateOf<GroupBalancesDto?>(null) }
    var settlements by remember { mutableStateOf<List<SettlementDto>>(emptyList()) }
    
    // Settlement form state
    var debtorId by remember { mutableStateOf<Long?>(null) }
    var creditorId by remember { mutableStateOf<Long?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PLN") }

    val scope = rememberCoroutineScope()

    fun loadData() {
        scope.launch {
            AppState.isLoading = true
            try {
                group = ApiClient.getGroupDetails(groupId)
                balancesDto = ApiClient.getBalances(groupId)
                settlements = ApiClient.getSettlements(groupId)
            } catch (e: Exception) {
                AppState.showError("Nie udało się pobrać danych rozliczeń.")
            } finally {
                AppState.isLoading = false
            }
        }
    }

    LaunchedEffect(groupId) {
        loadData()
    }

    val currentGroup = group
    val currentBalances = balancesDto

    if (currentGroup == null || currentBalances == null) {
        BaseScreenLayout(title = "Rozliczenia grupy", showBackButton = true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PremiumTheme.NeonCyan)
            }
        }
        return
    }

    val members = currentGroup.members

    BaseScreenLayout(
        title = "Bilanse i Rozliczenia: ${currentGroup.name}",
        showBackButton = true
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Member Net Balances
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "Bilanse netto członków",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                GlassCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (currentBalances.balances.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Brak aktywności finansowej w tej walucie.", color = PremiumTheme.TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(currentBalances.balances) { memberBalance ->
                                val user = memberBalance.user
                                val bal = memberBalance.netBalance
                                val curr = memberBalance.currency
                                
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(user.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        
                                        val isPositive = bal > 0.0
                                        val isZero = kotlin.math.abs(bal) < 0.01
                                        Text(
                                            text = if (isZero) "0.00 $curr" else if (isPositive) "+$bal $curr" else "$bal $curr",
                                            color = if (isZero) Color.White else if (isPositive) PremiumTheme.EmeraldGreen else PremiumTheme.CoralRed,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Custom visual balance bar
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x11FFFFFF))
                                    ) {
                                        // Draw a simple proportional bar
                                        val absoluteBal = kotlin.math.abs(bal)
                                        val maxVal = currentBalances.balances.maxOfOrNull { kotlin.math.abs(it.netBalance) } ?: 1.0
                                        val fraction = if (maxVal > 0.0) (absoluteBal / maxVal).toFloat() else 0f
                                        
                                        val alignment = if (bal >= 0.0) Alignment.CenterStart else Alignment.CenterEnd
                                        val barColor = if (bal == 0.0) Color.Transparent else if (bal > 0.0) PremiumTheme.EmeraldGreen else PremiumTheme.CoralRed

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .align(alignment)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(barColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Column: Simplified Debts & Settlements
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
            ) {
                // Simplified Transfers List
                Text(
                    text = "Sugerowane spłaty długów",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .clip(PremiumTheme.CardShape)
                        .background(PremiumTheme.GlassCardBg)
                        .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape)
                        .padding(20.dp)
                ) {
                    if (currentBalances.simplifiedDebts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Wszystkie długi są rozliczone! Grupa jest zbalansowana. ✨",
                                color = PremiumTheme.EmeraldGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentBalances.simplifiedDebts) { debt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x06FFFFFF))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(debt.fromUser.name, color = PremiumTheme.SoftPink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(" ➔ ", color = PremiumTheme.TextSecondary, fontSize = 13.sp)
                                            Text(debt.toUser.name, color = PremiumTheme.EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "Kwota długu: ${debt.amount} ${debt.currency}",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    // Quick Settle Action Button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PremiumTheme.NeonCyan)
                                            .clickable {
                                                debtorId = debt.fromUser.id
                                                creditorId = debt.toUser.id
                                                amountStr = debt.amount.toString()
                                                currency = debt.currency
                                            }
                                            .padding(vertical = 8.dp, horizontal = 12.dp)
                                    ) {
                                        Text("Rozlicz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Right splits: Quick Settlement Form (Left) & Settlement History (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Settlement Creator Form
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Zarejestruj spłatę", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            // Payer/Debtor drop label
                            val activeDebtor = members.find { it.id == debtorId }
                            val activeCreditor = members.find { it.id == creditorId }

                            Text(
                                text = "Kto spłaca: ${activeDebtor?.name ?: "(kliknij sugerowany dług)"}",
                                color = PremiumTheme.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Kto odbiera: ${activeCreditor?.name ?: "(kliknij sugerowany dług)"}",
                                color = PremiumTheme.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PremiumTextField(
                                    value = amountStr,
                                    onValueChange = { amountStr = it },
                                    label = "Kwota spłaty",
                                    placeholder = "0.00",
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumTextField(
                                    value = currency,
                                    onValueChange = { currency = it },
                                    label = "Waluta",
                                    placeholder = "PLN",
                                    modifier = Modifier.width(70.dp)
                                )
                            }

                            GlowButton(
                                text = "Wyślij spłatę",
                                onClick = {
                                    val dId = debtorId
                                    val cId = creditorId
                                    val payVal = amountStr.toDoubleOrNull()
                                    if (dId == null || cId == null || payVal == null || payVal <= 0.0) {
                                        AppState.showError("Wybierz dłużnika, wierzyciela oraz podaj poprawną kwotę!")
                                        return@GlowButton
                                    }

                                    scope.launch {
                                        AppState.isLoading = true
                                        AppState.clearMessages()
                                        try {
                                            ApiClient.createSettlement(
                                                groupId = groupId,
                                                request = CreateSettlementRequest(
                                                    amount = payVal,
                                                    currency = currency,
                                                    debtorId = dId,
                                                    creditorId = cId
                                                )
                                            )
                                            amountStr = ""
                                            AppState.showSuccess("Wysłano spłatę. Wierzyciel musi potwierdzić jej otrzymanie!")
                                            loadData()
                                        } catch (e: Exception) {
                                            AppState.showError("Błąd serwera przy rejestracji spłaty.")
                                        } finally {
                                            AppState.isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = PremiumTheme.EmeraldGreen,
                                enabled = debtorId != null && creditorId != null
                            )
                        }
                    }

                    // Settlements History List
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("Historia rozliczeń (${settlements.size})", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(PremiumTheme.CardShape)
                                .background(PremiumTheme.GlassCardBg)
                                .border(1.dp, PremiumTheme.GlowBorder, PremiumTheme.CardShape)
                                .padding(14.dp)
                        ) {
                            if (settlements.isEmpty() && !AppState.isLoading) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Brak zgłoszonych spłat.", color = PremiumTheme.TextSecondary, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(settlements) { settlement ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0x06FFFFFF))
                                                .padding(10.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "${settlement.debtor.name} ➔ ${settlement.creditor.name}",
                                                            color = PremiumTheme.TextPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "${settlement.amount} ${settlement.currency}",
                                                            color = PremiumTheme.NeonCyan,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        )
                                                    }
                                                    
                                                    // Approved / Pending Action
                                                    if (settlement.approved) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0x2210B981))
                                                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                                        ) {
                                                            Text("Zatwierdzona", color = PremiumTheme.EmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        val isCreditor = settlement.creditor.id == AppState.currentUser?.id
                                                        if (isCreditor) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(PremiumTheme.EmeraldGreen)
                                                                    .clickable {
                                                                        scope.launch {
                                                                            AppState.isLoading = true
                                                                            AppState.clearMessages()
                                                                            try {
                                                                                ApiClient.approveSettlement(groupId, settlement.id)
                                                                                AppState.showSuccess("Potwierdzono otrzymanie spłaty!")
                                                                                loadData()
                                                                            } catch (e: Exception) {
                                                                                AppState.showError("Nie udało się zatwierdzić spłaty.")
                                                                            } finally {
                                                                                AppState.isLoading = false
                                                                            }
                                                                        }
                                                                    }
                                                                    .padding(vertical = 6.dp, horizontal = 10.dp)
                                                            ) {
                                                                Text("Potwierdź", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        } else {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0x22EF4444))
                                                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                                                            ) {
                                                                Text("Oczekuje", color = PremiumTheme.CoralRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
        }
    }
}

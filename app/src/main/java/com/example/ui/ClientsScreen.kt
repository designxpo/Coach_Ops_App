package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import com.example.data.Client
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberWarning

enum class SortMode(val label: String, val emoji: String) {
    CONSISTENCY_LOW("Low Consistency First", "⚠️"),
    NAME_AZ("Name A → Z", "🔤"),
    NAME_ZA("Name Z → A", "🔡"),
    MRR_HIGH("Highest MRR", "💰"),
    MRR_LOW("Lowest MRR", "📉"),
    PROGRAM_WEEK("Program Week ↑", "📅"),
    LAST_ACTIVE("Last Check-in", "🕐")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: MainViewModel, onClientClick: (String) -> Unit, onChatClick: ((memberId: String, memberName: String, memberPhone: String) -> Unit)? = null) {
    val allClients by viewModel.clients.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val loggedTodayIds by viewModel.clientsLoggedToday.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }
    var sortMode by remember { mutableStateOf(SortMode.CONSISTENCY_LOW) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val filters = listOf("All", "Active", "At Risk", "New", "Paused")

    val filteredClients = allClients
        .filter { client ->
            val matchesSearch = searchQuery.isEmpty() ||
                client.name.contains(searchQuery, ignoreCase = true) ||
                client.phoneNumber.contains(searchQuery)
            val now = System.currentTimeMillis()
            val matchesFilter = when (activeFilter) {
                "All"     -> true
                "At Risk" -> payments.any { p ->
                    p.clientId == client.id && p.dueDateMillis > 0 &&
                    run { val daysFromDue = (p.dueDateMillis - now) / 86400000L; daysFromDue in -7..7 }
                }
                else      -> client.status.equals(activeFilter, ignoreCase = true)
            }
            matchesSearch && matchesFilter
        }
        .let { list ->
            when (sortMode) {
                SortMode.CONSISTENCY_LOW -> list.sortedBy { it.consistencyScore }
                SortMode.NAME_AZ         -> list.sortedBy { it.name.lowercase() }
                SortMode.NAME_ZA         -> list.sortedByDescending { it.name.lowercase() }
                SortMode.MRR_HIGH        -> list.sortedByDescending { it.mrr }
                SortMode.MRR_LOW         -> list.sortedBy { it.mrr }
                SortMode.PROGRAM_WEEK    -> list.sortedByDescending { it.programWeek }
                SortMode.LAST_ACTIVE     -> list.sortedByDescending { it.lastCheckInMillis }
            }
        }

    val sectionTitle = when {
        searchQuery.isNotEmpty() -> "Results"
        activeFilter == "All"   -> "All Clients"
        activeFilter == "At Risk" -> "At-Risk Clients"
        else                    -> "$activeFilter Clients"
    }

    if (showAddSheet) {
        AddClientSheet(
            programs = programs,
            onDismiss = { showAddSheet = false },
            onSave = { name, phone, goal, mrr, paymentCycle, programId, trainingStartDateMillis, trainingEndDateMillis ->
                viewModel.addClient(name, phone, goal, mrr, programId, paymentCycle,
                    trainingStartDateMillis = trainingStartDateMillis,
                    trainingEndDateMillis = trainingEndDateMillis)
                showAddSheet = false
            }
        )
    }

    if (showSortSheet) {
        ClientSortSheet(
            currentSort = sortMode,
            onSelect = { sortMode = it; showSortSheet = false },
            onDismiss = { showSortSheet = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Members", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberAccent)
                        .clickable { showAddSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Client", tint = CyberAccentDark, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                // Sort button — highlighted when non-default sort is active
                val isCustomSort = sortMode != SortMode.CONSISTENCY_LOW
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(if (isCustomSort) CyberAccent.copy(alpha = 0.18f) else CyberBgCard)
                        .border(1.dp, if (isCustomSort) CyberAccent.copy(alpha = 0.4f) else Color.Transparent, CircleShape)
                        .clickable { showSortSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Sort",
                        tint = if (isCustomSort) CyberAccent else CyberTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberBgCard)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = CyberTextPrimary, fontSize = 14.sp, fontFamily = FontFamily.Default),
                    cursorBrush = SolidColor(CyberAccent),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text("Search by name or phone…", fontSize = 14.sp, color = CyberTextMuted)
                        inner()
                    }
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val isActive = filter == activeFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isActive) CyberAccent.copy(alpha = 0.12f) else CyberBgCard)
                            .border(1.dp,
                                if (isActive) CyberAccent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                                RoundedCornerShape(999.dp))
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(filter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isActive) CyberAccent else CyberTextSecondary)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(CyberAccent).padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Explore your roster", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap any member to see their full profile.", fontSize = 13.sp, color = Color(0xFF0A0A0A).copy(alpha = 0.6f))
                }
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0x26000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = CyberAccentDark, modifier = Modifier.size(24.dp))
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sectionTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (sortMode != SortMode.CONSISTENCY_LOW) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(CyberAccent.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(sortMode.emoji, fontSize = 11.sp)
                        }
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(CyberBgCardElevated)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("${filteredClients.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                    }
                }
            }
        }

        if (filteredClients.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CyberBgCard),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👥", fontSize = 32.sp)
                        Text(if (searchQuery.isBlank() && activeFilter == "All") "No clients yet" else "No clients match your filter",
                            color = CyberTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        if (searchQuery.isBlank() && activeFilter == "All") {
                            Text("Tap + to add your first member", color = CyberTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        items(filteredClients, key = { it.id }) { client ->
            val program = programs.find { it.id == client.programId }
            ClientCard(
                client = client,
                programName = program?.name,
                isLoggedToday = client.id in loggedTodayIds,
                onMarkAttendance = { viewModel.markAttendanceToday(client, program) },
                onClientClick = { onClientClick(client.id) },
                onChatClick = if (onChatClick != null) {
                    { onChatClick(client.id, client.name, client.phoneNumber) }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientSortSheet(
    currentSort: SortMode,
    onSelect: (SortMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberBgCard,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Sort Members", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                    Text("Choose how to order your roster", fontSize = 12.sp, color = CyberTextMuted)
                }
            }
            Spacer(Modifier.height(16.dp))

            SortMode.entries.forEach { mode ->
                val isSelected = mode == currentSort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(mode) }
                        .background(if (isSelected) CyberAccent.copy(alpha = 0.08f) else Color.Transparent)
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(mode.emoji, fontSize = 20.sp)
                    Text(
                        mode.label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) CyberAccent else CyberTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private val avatarPalette = listOf(
    Color(0xFFC5F23E), Color(0xFF38BDF8), Color(0xFFA78BFA),
    Color(0xFFFB923C), Color(0xFF34D399), Color(0xFFF472B6),
    Color(0xFF60A5FA), Color(0xFFFBBF24),
)

private fun avatarColor(name: String): Color =
    avatarPalette[name.hashCode().and(0x7FFFFFFF) % avatarPalette.size]

@Composable
fun ClientCard(
    client: Client,
    programName: String?,
    isLoggedToday: Boolean = false,
    onMarkAttendance: () -> Unit = {},
    onClientClick: () -> Unit,
    onChatClick: (() -> Unit)? = null
) {
    val ringColor = when (client.status) {
        "Active" -> CyberSuccess
        "Paused" -> CyberWarning
        else     -> CyberDanger
    }
    val avColor = avatarColor(client.name)
    val locationText = client.city.ifEmpty { "" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CyberBgCard)
            .clickable { onClientClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).border(2.dp, ringColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp)).background(avColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(client.name.take(1), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = avColor)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(client.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            val sdf = remember { java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()) }
            val enrolledDate = sdf.format(java.util.Date(client.enrollmentDateMillis))
            Text("Enrolled $enrolledDate", fontSize = 11.sp, color = CyberTextMuted)
            Spacer(Modifier.height(4.dp))
            if (locationText.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.LocationOn, null, tint = CyberTextSecondary, modifier = Modifier.size(12.dp))
                    Text(locationText, fontSize = 12.sp, color = CyberTextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val programDisplay = programName?.take(16) ?: "No Program"
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(avColor.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text(programDisplay, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = avColor)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(ringColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text("W${client.programWeek}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ringColor)
                }
            }
        }

        // Quick attendance button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLoggedToday) CyberSuccess.copy(alpha = 0.20f)
                        else Color.White.copy(alpha = 0.06f)
                    )
                    .clickable(enabled = !isLoggedToday) { onMarkAttendance() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedToday) {
                    Icon(Icons.Filled.Check, contentDescription = "Attended today",
                        tint = CyberSuccess, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Mark attendance",
                        tint = CyberTextMuted, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                if (isLoggedToday) "Done" else "Log",
                fontSize = 9.sp,
                color = if (isLoggedToday) CyberSuccess else CyberTextMuted,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (onChatClick != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(CyberAccent.copy(alpha = 0.15f))
                    .clickable { onChatClick() },
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Chat",
                        tint = CyberAccent, modifier = Modifier.size(18.dp))
                }
                Text("Chat", fontSize = 9.sp, color = CyberAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

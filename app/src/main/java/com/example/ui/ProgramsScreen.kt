package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Program
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgCardElevated
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgramsScreen(viewModel: MainViewModel, onProgramClick: (String) -> Unit = {}) {
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val totalSessions by viewModel.totalCompletedSessions.collectAsStateWithLifecycle()
    val sevenDaysAgo = remember { System.currentTimeMillis() - 7 * 86400000L }
    val sessionsThisWeek by viewModel.getSessionsSince(sevenDaysAgo).collectAsStateWithLifecycle(0)
    val sparklineBars by viewModel.sparklineBars.collectAsStateWithLifecycle()
    var showAssignSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var programToEdit by remember { mutableStateOf<com.example.data.Program?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    if (showAssignSheet) {
        AssignProgramSheet(
            programs = programs,
            clients = clients,
            onDismiss = { showAssignSheet = false },
            onAssign = { clientIds, programId ->
                clientIds.forEach { viewModel.assignProgram(it, programId) }
                showAssignSheet = false
            }
        )
    }

    if (showCreateSheet) {
        ProgramFormSheet(
            onDismiss = { showCreateSheet = false },
            onSave = { name, weeks, days, tags, workingDays ->
                viewModel.addProgramFromTemplate(name, weeks, days, tags, workingDays)
                showCreateSheet = false
            }
        )
    }

    programToEdit?.let { prog ->
        ProgramFormSheet(
            existingProgram = prog,
            onDismiss = { programToEdit = null },
            onSave = { name, weeks, days, tags, workingDays ->
                if (prog.id.startsWith("TEMP_")) {
                    viewModel.addProgramFromTemplate(name, weeks, days, tags, workingDays)
                } else {
                    viewModel.updateProgram(prog.copy(name = name, durationWeeks = weeks, daysPerWeek = days, tags = tags, workingDays = workingDays))
                }
                programToEdit = null
            }
        )
    }

    val filteredPrograms = if (searchQuery.isBlank()) programs
        else programs.filter { it.name.contains(searchQuery, ignoreCase = true) || it.tags.contains(searchQuery, ignoreCase = true) }

    val avgMrr = if (clients.isNotEmpty()) clients.map { it.mrr }.average().toInt() else 0
    val avgMrrDisplay = when {
        avgMrr >= 1000 -> "₹${"%,d".format(avgMrr)}"
        else -> "₹$avgMrr"
    }
    val avgDuration = if (programs.isNotEmpty()) programs.map { it.durationWeeks }.average().toInt() else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary).statusBarsPadding(),
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
                    Icon(Icons.Filled.History, contentDescription = null, tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Program Tracker", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberAccent)
                        .clickable { showCreateSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Program", tint = CyberAccentDark, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Search bar
        item {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    .background(CyberBgCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Search, null, tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(color = CyberTextPrimary, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CyberAccent),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text("Search programs…", fontSize = 14.sp, color = CyberTextMuted)
                            inner()
                        }
                    }
                )
            }
        }

        // Bento grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Session History tile — lime, tall
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(CyberAccent)
                    ) {
                        Icon(
                            Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF0A0A0A).copy(alpha = 0.12f),
                            modifier = Modifier.size(56.dp).align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, tint = CyberAccentDark.copy(alpha = 0.65f), modifier = Modifier.size(13.dp))
                                Text("Sessions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyberAccentDark.copy(alpha = 0.65f), letterSpacing = 0.5.sp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "$totalSessions",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyberAccentDark,
                                    lineHeight = 32.sp
                                )
                                Text("total logged", fontSize = 11.sp, color = CyberAccentDark.copy(alpha = 0.65f))
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x26000000))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = CyberAccentDark, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "+$sessionsThisWeek this week",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberAccentDark,
                                    style = androidx.compose.ui.text.TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                                        lineHeight = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    // Live Tracking tile — dark with sparkline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(CyberBgCard)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Timeline, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                SparklineChart(bars = sparklineBars)
                                Spacer(Modifier.height(6.dp))
                                Text("Live Tracking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                                Text("This week's adherence", fontSize = 11.sp, color = CyberTextMuted)
                            }
                        }
                    }
                }

                // Right column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // MRR/client stat — dark, short
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(CyberBgCard)
                            .padding(16.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AttachMoney, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text(avgMrrDisplay, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                                Text("MRR / member", fontSize = 11.sp, color = CyberTextMuted)
                            }
                        }
                    }

                    // Duration stat — dark, short
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(CyberBgCard)
                            .padding(16.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                Text("$avgDuration wks", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                                Text("Avg duration", fontSize = 11.sp, color = CyberTextMuted)
                            }
                        }
                    }

                    // Sessions stat — dark, short
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(CyberBgCard)
                            .padding(16.dp)
                    ) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(CyberAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(16.dp))
                            }
                            Column {
                                val modalDaysPerWeek = programs.mapNotNull { it.daysPerWeek }.groupBy { it }.maxByOrNull { it.value.size }?.key ?: 0
                                Text("$modalDaysPerWeek / wk", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = CyberTextPrimary)
                                Text("Sessions/week", fontSize = 11.sp, color = CyberTextMuted)
                            }
                        }
                    }
                }
            }
        }

        // Assign button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(CyberAccent)
                    .clickable { showAssignSheet = true }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = CyberAccentDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Assign New Program", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
            }
        }

        // Section header
        item {
            Text("Your Programs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary,
                modifier = Modifier.padding(top = 4.dp))
        }

        if (filteredPrograms.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        .background(CyberBgCard),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📋", fontSize = 28.sp)
                        Text(if (searchQuery.isBlank()) "No programs yet" else "No programs match \"$searchQuery\"",
                            color = CyberTextSecondary, fontSize = 14.sp)
                        if (searchQuery.isBlank()) {
                            Text("Tap + to create your first program", color = CyberTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        items(filteredPrograms, key = { it.id }) { program ->
            val programClients = clients.filter { it.programId == program.id }.map { it.name.take(1) }
            AssignmentCard(
                program = program,
                programClients = programClients,
                onClone = { viewModel.cloneProgram(program) },
                onEdit = { programToEdit = program },
                onClick = { onProgramClick(program.id) }
            )
        }

        // Template Library
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Starter Templates", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Industry-proven programs — one tap to add to your library",
                    fontSize = 12.sp, color = CyberTextMuted)
            }
        }

        items(starterTemplates, key = { it.name }) { template ->
            TemplateCard(
                template = template,
                onUse = {
                    viewModel.addProgramFromTemplate(template.name, template.weeks, template.daysPerWeek, template.tags)
                },
                onCustomise = {
                    // Pre-populate the create sheet with template values
                    showCreateSheet = false
                    programToEdit = com.example.data.Program(
                        id = "TEMP_${template.name}",
                        name = template.name,
                        durationWeeks = template.weeks,
                        daysPerWeek = template.daysPerWeek,
                        tags = template.tags
                    )
                }
            )
        }
    }
}

@Composable
fun SparklineChart(bars: List<Float> = List(10) { 0.05f }) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { value ->
            val isHighlight = value > 0.7f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(value)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isHighlight) CyberAccent else CyberAccent.copy(alpha = 0.22f))
            )
        }
    }
}

@Composable
fun AssignmentCard(program: Program, programClients: List<String> = emptyList(), onClone: () -> Unit, onEdit: () -> Unit = {}, onClick: () -> Unit = {}) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateStr = dateFormatter.format(Date(program.lastUsedMillis)).uppercase()
    val visibleInitials = programClients.take(3)
    val overflow = (program.clientCount - visibleInitials.size).coerceAtLeast(0)
    val clientInitials = if (overflow > 0) visibleInitials + "+$overflow" else visibleInitials.ifEmpty { listOf("—") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CyberBgCard)
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(program.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$dateStr · ${program.clientCount} CLIENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyberTextMuted,
                    letterSpacing = 0.4.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            OverlappingAvatars(clientInitials)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyberBgCardElevated)
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = CyberAccent, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CyberBgCardElevated)
                    .clickable { onClone() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Clone", tint = CyberTextMuted, modifier = Modifier.size(14.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill(icon = { Icon(Icons.Filled.AccessTime, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(12.dp)) }, label = "${program.durationWeeks} weeks")
            StatPill(icon = { Icon(Icons.Filled.Bolt, contentDescription = null, tint = CyberAccent, modifier = Modifier.size(12.dp)) }, label = "${program.daysPerWeek} sessions/wk")
        }
    }
}

@Composable
fun OverlappingAvatars(initials: List<String>) {
    val avatarSize = 32.dp
    val step = 20.dp
    val count = initials.size
    val totalWidth = avatarSize + step * (count - 1)

    Box(modifier = Modifier.width(totalWidth).height(avatarSize)) {
        initials.forEachIndexed { index, initial ->
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .zIndex((initials.size - index).toFloat())
                    .size(avatarSize)
                    .background(CyberBgCard, CircleShape) // white border via background trick
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(CyberBgCardElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
            }
        }
    }
}

@Composable
fun StatPill(icon: @Composable () -> Unit, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CyberBgCardElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CyberTextSecondary)
    }
}

private data class ProgramTemplate(
    val emoji: String,
    val name: String,
    val weeks: Int,
    val daysPerWeek: Int,
    val tags: String,
    val description: String
)

private val starterTemplates = listOf(
    ProgramTemplate("🏋️", "Strength Foundation 5x5",         16, 3, "Strength,Intermediate",  "Classic barbell protocol with progressive overload. Builds a raw strength base in 16 weeks."),
    ProgramTemplate("🔥", "Fat Loss 8-Week Blast",            8,  4, "Fat Loss,Beginner",       "HIIT + resistance combo designed for 0.5–1kg weekly loss. Cardio and compound lifts."),
    ProgramTemplate("💪", "Hypertrophy Push-Pull-Legs",       12, 4, "Hypertrophy,Advanced",    "Science-backed PPL split with high volume. Optimised for maximum muscle hypertrophy."),
    ProgramTemplate("🧘", "Mobility & Recovery Reset",        4,  5, "Mobility,Recovery",       "Corrective exercises, flexibility work, and breathwork. Ideal post-injury or active recovery."),
    ProgramTemplate("⚡", "Sports Performance Edge",          10, 5, "Athletic,Performance",    "Power, speed, agility, and VO2 max training. Used by competitive athletes across sports."),
    ProgramTemplate("🌟", "General Wellness 12-Week",         12, 3, "Wellness,Beginner",       "Sustainable habit building balancing cardio, strength, and flexibility. Perfect starter program.")
)

@Composable
private fun TemplateCard(template: ProgramTemplate, onUse: () -> Unit, onCustomise: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CyberBgCard)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(template.emoji, fontSize = 24.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(template.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(template.description, fontSize = 11.sp, color = CyberTextMuted, maxLines = 2, lineHeight = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatPill(icon = { Icon(Icons.Filled.AccessTime, null, tint = CyberAccent, modifier = Modifier.size(11.dp)) }, label = "${template.weeks} wks")
                StatPill(icon = { Icon(Icons.Filled.Bolt, null, tint = CyberAccent, modifier = Modifier.size(11.dp)) }, label = "${template.daysPerWeek}x/wk")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberAccent)
                    .clickable { onUse() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Use", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = CyberAccentDark)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBgCardElevated)
                    .clickable { onCustomise() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, null, tint = CyberAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

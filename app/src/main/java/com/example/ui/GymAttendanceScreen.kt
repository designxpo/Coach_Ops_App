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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberAccentDark
import com.example.ui.theme.CyberBgCard
import com.example.ui.theme.CyberBgPrimary
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GymAttendanceScreen(viewModel: GymViewModel, onBack: () -> Unit) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val todayCheckIns by viewModel.todayCheckIns.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    val checkedInIds = todayCheckIns.map { it.memberId }.toSet()
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val searchResults = if (searchQuery.isBlank()) emptyList() else members.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CyberBgPrimary),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(CyberBgCard).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = CyberTextPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Attendance", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = CyberTextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CyberAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("${todayCheckIns.size} today", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberAccent)
                }
            }
        }

        // Search to check in
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search member to check in…", color = CyberTextMuted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CyberTextPrimary,
                    unfocusedTextColor = CyberTextPrimary,
                    focusedBorderColor = CyberAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    cursorColor = CyberAccent,
                    focusedContainerColor = CyberBgCard,
                    unfocusedContainerColor = CyberBgCard
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Search results with check-in buttons
        searchResults.forEach { member ->
            item(key = "search_${member.id}") {
                val alreadyIn = member.id in checkedInIds
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(member.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberTextPrimary)
                        if (member.isExpired) {
                            Text("⚠️ Membership expired", fontSize = 11.sp, color = CyberDanger, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(member.planName.ifEmpty { "No plan" }, fontSize = 11.sp, color = CyberTextMuted)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (alreadyIn) CyberSuccess.copy(alpha = 0.15f) else CyberAccent)
                            .clickable(enabled = !alreadyIn) {
                                viewModel.checkIn(member)
                                searchQuery = ""
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (alreadyIn) "✓ In" else "Check In",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (alreadyIn) CyberSuccess else CyberAccentDark
                        )
                    }
                }
            }
        }

        item {
            Text("Today's Check-ins", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = CyberTextPrimary, modifier = Modifier.padding(top = 8.dp))
        }

        if (todayCheckIns.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏋️", fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No check-ins yet today", color = CyberTextSecondary, fontSize = 14.sp)
                    Text("Search a member above to mark attendance", color = CyberTextMuted, fontSize = 12.sp)
                }
            }
        }

        todayCheckIns.forEach { checkIn ->
            item(key = checkIn.id) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CyberBgCard).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = CyberSuccess, modifier = Modifier.size(20.dp))
                    Text(checkIn.memberName, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = CyberTextPrimary, modifier = Modifier.weight(1f))
                    Text(timeFmt.format(Date(checkIn.timeMillis)), fontSize = 12.sp, color = CyberTextMuted)
                }
            }
        }
    }
}

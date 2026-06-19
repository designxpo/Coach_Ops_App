package com.example.data

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val type: String = "text",   // "text" | "reminder"
    val read: Boolean = false
)

data class ChatThread(
    val id: String = "",
    val coachId: String = "",
    val memberId: String = "",
    val coachName: String = "",
    val memberName: String = "",
    val memberPhone: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCoach: Int = 0,    // messages coach hasn't read yet (member sent these)
    val unreadMember: Int = 0    // messages member hasn't read yet (coach sent these)
)

data class ReminderTemplate(
    val emoji: String,
    val title: String,
    val body: String       // {name} will be replaced with member's name
)

val REMINDER_TEMPLATES = listOf(
    ReminderTemplate("🏋️", "Workout Reminder",
        "Hey {name}! Your workout session is scheduled for today. Let's crush it! 💪"),
    ReminderTemplate("💧", "Hydration Check-in",
        "Hi {name}! Don't forget to drink enough water today — aim for {water}L. Stay hydrated! 💧"),
    ReminderTemplate("📊", "Weekly Progress Check",
        "Hey {name}! It's weekly check-in time. How are you progressing towards your goals? Let me know! 📈"),
    ReminderTemplate("😴", "Rest Day Reminder",
        "Hi {name}! Today is your rest day. Focus on recovery, sleep well, and eat right. See you tomorrow! 🙏"),
    ReminderTemplate("🥗", "Nutrition Reminder",
        "Hey {name}! Quick reminder to stay on track with your nutrition plan today. You're doing great! 🥗"),
    ReminderTemplate("🔥", "Motivation Boost",
        "Hi {name}! Just wanted to say — you're making incredible progress. Keep going, I'm proud of you! 🔥")
)

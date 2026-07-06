package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Personal food diary — every scanned/spoken/typed food can be logged here.
 * Daily totals vs macro targets, meal buckets and cheat-meal tracking.
 * Room is the source of truth; mirrors to client_fitness/{uid}/food_diary.
 */
@Entity(tableName = "food_diary")
data class FoodDiaryEntry(
    @PrimaryKey val id: String,
    val dateKey: String,                 // yyyy-MM-dd
    val timeMillis: Long = System.currentTimeMillis(),
    val mealType: String = "SNACKS",     // BREAKFAST, LUNCH, SNACKS, DINNER
    val foodName: String,
    val servingDesc: String = "",
    val calories: Int = 0,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
    val fiberG: Float = 0f,
    val isCheatMeal: Boolean = false,
    val source: String = ""              // AI, BARCODE, VOICE
)

@Dao
interface FoodDiaryDao {
    @Query("SELECT * FROM food_diary WHERE dateKey = :dateKey ORDER BY timeMillis ASC")
    fun entriesForDate(dateKey: String): Flow<List<FoodDiaryEntry>>

    @Query("SELECT * FROM food_diary WHERE dateKey BETWEEN :from AND :to")
    suspend fun entriesBetween(from: String, to: String): List<FoodDiaryEntry>

    @Query("SELECT COUNT(*) FROM food_diary")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodDiaryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<FoodDiaryEntry>)

    @Update
    suspend fun update(entry: FoodDiaryEntry)

    @Query("DELETE FROM food_diary WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM food_diary")
    suspend fun clearAll()
}

object FoodDiary {

    // Foods that read as an indulgence — auto-tagged so the diary can show
    // "cheat meals this week" honestly. The user can always toggle the tag.
    private val CHEAT_KEYWORDS = listOf(
        "gulab", "jalebi", "barfi", "ladoo", "laddu", "halwa", "rasgulla", "rasmalai",
        "ice cream", "cake", "cupcake", "pancake", "pastry", "brownie", "donut", "chocolate", "kheer",
        "samosa", "pakora", "kachori", "vada pav", "chole bhature", "bhatura",
        "pizza", "burger", "french fries", "fries", "spring roll", "frankie",
        "chips", "maggi", "momo", "pav bhaji", "dabeli", "puri"
    )

    // Whole-word matching — "cake" must not flag "pancake" (its own keyword now),
    // "puri" must not flag "purine"
    private val CHEAT_REGEXES = CHEAT_KEYWORDS.map { Regex("\\b${Regex.escape(it)}\\b") }

    fun dayKey(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun mealTypeForNow(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 11 -> "BREAKFAST"
            hour < 16 -> "LUNCH"
            hour < 19 -> "SNACKS"
            else      -> "DINNER"
        }
    }

    // 800 kcal, not 650: a normal full Indian dinner (dal + rice + sabzi + roti)
    // commonly lands 650–750 and shouldn't be branded a cheat meal
    fun looksLikeCheatMeal(name: String, calories: Int): Boolean {
        val n = name.lowercase()
        return calories >= 800 || CHEAT_REGEXES.any { it.containsMatchIn(n) }
    }

    /** Builds an entry from a scanner result — meal bucket and cheat flag auto-set. */
    fun entryFrom(n: FoodNutrition, source: String): FoodDiaryEntry =
        FoodDiaryEntry(
            id = UUID.randomUUID().toString(),
            dateKey = dayKey(),
            mealType = mealTypeForNow(),
            foodName = n.foodName,
            servingDesc = n.servingSize,
            calories = n.calories,
            proteinG = n.proteinG,
            carbsG = n.carbsG,
            fatG = n.fatG,
            fiberG = n.fiberG,
            isCheatMeal = looksLikeCheatMeal(n.foodName, n.calories),
            source = source
        )

    // ─── Firestore mirror (best-effort, offline-safe) ─────────────────────────

    private fun col() = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
        FirebaseFirestore.getInstance()
            .collection("client_fitness").document(uid).collection("food_diary")
    }

    fun syncSave(e: FoodDiaryEntry) {
        col()?.document(e.id)?.set(mapOf(
            "id" to e.id, "dateKey" to e.dateKey, "timeMillis" to e.timeMillis,
            "mealType" to e.mealType, "foodName" to e.foodName,
            "servingDesc" to e.servingDesc, "calories" to e.calories,
            "proteinG" to e.proteinG.toDouble(), "carbsG" to e.carbsG.toDouble(),
            "fatG" to e.fatG.toDouble(), "fiberG" to e.fiberG.toDouble(),
            "isCheatMeal" to e.isCheatMeal, "source" to e.source
        ))
    }

    fun syncDelete(id: String) {
        col()?.document(id)?.delete()
    }

    /** Restores the diary on a new device / reinstall (last 30 days). */
    suspend fun pullToRoomIfEmpty(dao: FoodDiaryDao) {
        if (dao.count() > 0) return
        val collection = col() ?: return
        try {
            val cutoff = dayKey(Date(System.currentTimeMillis() - 30L * 86400000L))
            val docs = collection.whereGreaterThanOrEqualTo("dateKey", cutoff).get().await()
            val entries = docs.documents.mapNotNull { d ->
                FoodDiaryEntry(
                    id = d.getString("id") ?: return@mapNotNull null,
                    dateKey = d.getString("dateKey") ?: "",
                    timeMillis = d.getLong("timeMillis") ?: 0L,
                    mealType = d.getString("mealType") ?: "SNACKS",
                    foodName = d.getString("foodName") ?: "",
                    servingDesc = d.getString("servingDesc") ?: "",
                    calories = (d.getLong("calories") ?: 0L).toInt(),
                    proteinG = (d.getDouble("proteinG") ?: 0.0).toFloat(),
                    carbsG = (d.getDouble("carbsG") ?: 0.0).toFloat(),
                    fatG = (d.getDouble("fatG") ?: 0.0).toFloat(),
                    fiberG = (d.getDouble("fiberG") ?: 0.0).toFloat(),
                    isCheatMeal = d.getBoolean("isCheatMeal") ?: false,
                    source = d.getString("source") ?: ""
                )
            }
            if (entries.isNotEmpty()) dao.insertAll(entries)
        } catch (_: Exception) { /* offline — Room stays authoritative */ }
    }
}

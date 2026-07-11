package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Client::class,
        Signal::class,
        Program::class,
        Payment::class,
        WorkoutLog::class,
        BodyMeasurement::class,
        ClientNote::class,
        RevenueSnapshot::class,
        Gym::class,
        GymMember::class,
        GymPlan::class,
        GymPayment::class,
        GymCheckIn::class,
        FoodDiaryEntry::class,
    ],
    // v18: multi-gym (gyms table + gymId columns). Destructive fallback is the
    // project convention — gym & coach data re-pull from Firestore on next open.
    version = 18,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachDao(): CoachDao
    abstract fun gymDao(): GymDao
    abstract fun foodDiaryDao(): FoodDiaryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coachops_db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
    }
}

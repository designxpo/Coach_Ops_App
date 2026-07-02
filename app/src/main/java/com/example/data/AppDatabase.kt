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
        WaterEntry::class,
        GymMember::class,
        GymPlan::class,
        GymPayment::class,
        GymCheckIn::class,
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachDao(): CoachDao
    abstract fun waterDao(): WaterDao
    abstract fun gymDao(): GymDao

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

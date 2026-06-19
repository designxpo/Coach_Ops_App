package com.example.data

import androidx.room.Database
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
        RevenueSnapshot::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachDao(): CoachDao
}

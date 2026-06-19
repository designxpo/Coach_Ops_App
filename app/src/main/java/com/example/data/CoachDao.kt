package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoachDao {
    // Clients
    @Query("SELECT * FROM clients ORDER BY consistencyScore ASC")
    fun getAllClients(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :clientId")
    fun getClient(clientId: String): Flow<Client?>

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getClientCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: Client)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<Client>)

    @Update
    suspend fun updateClient(client: Client)

    @Query("UPDATE clients SET status = :status WHERE id = :clientId")
    suspend fun updateClientStatus(clientId: String, status: String)

    @Query("UPDATE clients SET programId = :programId, programWeek = 1, enrollmentDateMillis = :enrolledAt WHERE id = :clientId")
    suspend fun assignProgram(clientId: String, programId: String, enrolledAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM clients WHERE id = :clientId")
    suspend fun deleteClient(clientId: String)

    // Signals
    @Query("SELECT * FROM signals WHERE isResolved = 0 ORDER BY CASE severity WHEN 'RED' THEN 1 WHEN 'YELLOW' THEN 2 ELSE 3 END ASC, timestamp DESC")
    fun getUnresolvedSignals(): Flow<List<Signal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: Signal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<Signal>)

    @Query("UPDATE signals SET isResolved = 1 WHERE id = :signalId")
    suspend fun resolveSignal(signalId: String)

    @Query("UPDATE signals SET isResolved = 1")
    suspend fun resolveAllSignals()

    @Query("DELETE FROM signals WHERE type = :type")
    suspend fun deleteSignalsByType(type: String)

    @Query("DELETE FROM signals WHERE clientId = :clientId")
    suspend fun deleteSignalsForClient(clientId: String)

    // Programs
    @Query("SELECT * FROM programs WHERE isMasterTemplate = 1 ORDER BY lastUsedMillis DESC")
    fun getMasterPrograms(): Flow<List<Program>>

    @Query("SELECT * FROM programs WHERE id = :programId")
    fun getProgram(programId: String): Flow<Program?>

    @Query("SELECT * FROM clients WHERE programId = :programId ORDER BY consistencyScore ASC")
    fun getClientsForProgram(programId: String): Flow<List<Client>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: Program)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<Program>)

    @Update
    suspend fun updateProgram(program: Program)

    @Query("SELECT COUNT(*) FROM signals WHERE clientId = :clientId AND type = :type AND isResolved = 0")
    suspend fun hasUnresolvedSignalOfType(clientId: String, type: String): Int

    // Payments
    @Query("SELECT * FROM payments ORDER BY daysOverdue DESC, dueDateMillis ASC")
    fun getPendingPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY dueDateMillis DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)

    @Query("DELETE FROM payments WHERE clientId = :clientId")
    suspend fun deletePaymentsForClient(clientId: String)

    // WorkoutLogs
    @Query("SELECT * FROM workout_logs WHERE clientId = :clientId ORDER BY sessionDateMillis DESC")
    fun getWorkoutLogsForClient(clientId: String): Flow<List<WorkoutLog>>

    @Query("SELECT COUNT(*) FROM workout_logs WHERE clientId = :clientId")
    suspend fun getWorkoutLogCount(clientId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE clientId = :clientId")
    suspend fun deleteWorkoutLogsForClient(clientId: String)

    @Query("SELECT * FROM workout_logs WHERE programId = :programId ORDER BY sessionDateMillis DESC")
    fun getWorkoutLogsForProgram(programId: String): Flow<List<WorkoutLog>>

    @Query("SELECT COUNT(*) FROM workout_logs WHERE clientId = :clientId AND isMissed = 0")
    suspend fun getCompletedLogCount(clientId: String): Int

    @Query("SELECT COUNT(*) FROM workout_logs WHERE clientId = :clientId AND isMissed = 0 AND sessionDateMillis >= :since")
    suspend fun getCompletedLogCountSince(clientId: String, since: Long): Int

    @Query("SELECT DISTINCT clientId FROM workout_logs WHERE isMissed = 0 AND sessionDateMillis >= :since")
    fun getClientIdsWithLogSince(since: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM workout_logs WHERE isMissed = 0")
    fun getTotalCompletedSessions(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_logs WHERE isMissed = 0 AND sessionDateMillis >= :since")
    fun getSessionsSince(since: Long): Flow<Int>

    @Query("SELECT sessionDateMillis FROM workout_logs WHERE isMissed = 0 AND sessionDateMillis >= :since ORDER BY sessionDateMillis ASC")
    fun getSessionDatesSince(since: Long): Flow<List<Long>>

    // BodyMeasurements
    @Query("SELECT * FROM body_measurements WHERE clientId = :clientId ORDER BY dateMillis DESC")
    fun getMeasurementsForClient(clientId: String): Flow<List<BodyMeasurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurement)

    @Query("DELETE FROM body_measurements WHERE clientId = :clientId")
    suspend fun deleteMeasurementsForClient(clientId: String)

    // ClientNotes
    @Query("SELECT * FROM client_notes WHERE clientId = :clientId ORDER BY dateMillis DESC")
    fun getNotesForClient(clientId: String): Flow<List<ClientNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ClientNote)

    @Query("DELETE FROM client_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: String)

    @Query("DELETE FROM client_notes WHERE clientId = :clientId")
    suspend fun deleteNotesForClient(clientId: String)

    // RevenueSnapshots
    @Query("SELECT * FROM revenue_snapshots ORDER BY monthYear DESC LIMIT 6")
    fun getRecentSnapshots(): Flow<List<RevenueSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: RevenueSnapshot)

    // Low-consistency clients for notification worker
    @Query("SELECT * FROM clients WHERE consistencyScore < 40 AND status = 'Active'")
    suspend fun getLowConsistencyClients(): List<Client>

    @Query("SELECT * FROM payments WHERE (mandateStatus = 'FAILED' OR daysOverdue > 7)")
    suspend fun getOverduePayments(): List<Payment>

    // Clear all tables (used on logout so the next user starts fresh)
    @Query("DELETE FROM clients")          suspend fun clearClients()
    @Query("DELETE FROM signals")          suspend fun clearSignals()
    @Query("DELETE FROM programs")         suspend fun clearPrograms()
    @Query("DELETE FROM payments")         suspend fun clearPayments()
    @Query("DELETE FROM workout_logs")     suspend fun clearWorkoutLogs()
    @Query("DELETE FROM body_measurements")suspend fun clearMeasurements()
    @Query("DELETE FROM client_notes")     suspend fun clearNotes()
    @Query("DELETE FROM revenue_snapshots")suspend fun clearSnapshots()
}

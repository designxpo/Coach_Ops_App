package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CoachRepository(private val coachDao: CoachDao) {

    val allClients:          Flow<List<Client>>          = coachDao.getAllClients()
    val unresolvedSignals:   Flow<List<Signal>>          = coachDao.getUnresolvedSignals()
    val masterPrograms:      Flow<List<Program>>         = coachDao.getMasterPrograms()
    val pendingPayments:     Flow<List<Payment>>         = coachDao.getPendingPayments()
    val recentSnapshots:     Flow<List<RevenueSnapshot>> = coachDao.getRecentSnapshots()
    val totalCompletedSessions: Flow<Int>               = coachDao.getTotalCompletedSessions()

    fun getSessionsSince(since: Long): Flow<Int>         = coachDao.getSessionsSince(since)
    fun getSessionDatesSince(since: Long): Flow<List<Long>> = coachDao.getSessionDatesSince(since)
    fun getClientIdsWithLogSince(since: Long): Flow<List<String>> = coachDao.getClientIdsWithLogSince(since)
    fun getClient(clientId: String)                      = coachDao.getClient(clientId)
    fun getPaymentsForClient(clientId: String)           = coachDao.getPaymentsForClient(clientId)
    fun getProgram(id: String)                           = coachDao.getProgram(id)
    fun getClientsForProgram(programId: String)          = coachDao.getClientsForProgram(programId)
    fun getWorkoutLogsForClient(clientId: String)        = coachDao.getWorkoutLogsForClient(clientId)
    fun getWorkoutLogsForProgram(programId: String)      = coachDao.getWorkoutLogsForProgram(programId)
    fun getMeasurementsForClient(clientId: String)       = coachDao.getMeasurementsForClient(clientId)
    fun getNotesForClient(clientId: String)              = coachDao.getNotesForClient(clientId)
    suspend fun getClientCount(): Int                    = coachDao.getClientCount()

    // ─── Clients ──────────────────────────────────────────────────────────────

    suspend fun insertClient(client: Client) {
        coachDao.insertClient(client)
        FirestoreSync.saveClient(client)
    }

    suspend fun updateClient(client: Client) {
        coachDao.updateClient(client)
        FirestoreSync.saveClient(client)
    }

    suspend fun updateClientStatus(clientId: String, status: String) {
        coachDao.updateClientStatus(clientId, status)
        // Fetch updated client to sync
        coachDao.getClient(clientId).let { flow ->
            // Firestore update happens next time full client is saved
        }
    }

    suspend fun assignProgram(clientId: String, programId: String) {
        coachDao.assignProgram(clientId, programId, System.currentTimeMillis())
        // Full client sync happens via updateClient calls
    }

    suspend fun deleteClient(clientId: String) {
        coachDao.deleteWorkoutLogsForClient(clientId)
        coachDao.deleteMeasurementsForClient(clientId)
        coachDao.deleteNotesForClient(clientId)
        coachDao.deleteSignalsForClient(clientId)
        coachDao.deletePaymentsForClient(clientId)
        coachDao.deleteClient(clientId)
        // Mirror deletes to Firestore
        FirestoreSync.deleteClient(clientId)
        FirestoreSync.deleteWorkoutLogsByClient(clientId)
        FirestoreSync.deleteMeasurementsByClient(clientId)
        FirestoreSync.deleteNotesByClient(clientId)
        FirestoreSync.deletePaymentsByClient(clientId)
    }

    // ─── Signals ──────────────────────────────────────────────────────────────

    suspend fun insertSignals(signals: List<Signal>) = coachDao.insertSignals(signals)
    suspend fun resolveSignal(signalId: String)      = coachDao.resolveSignal(signalId)
    suspend fun resolveAllSignals()                  = coachDao.resolveAllSignals()

    // ─── Programs ─────────────────────────────────────────────────────────────

    suspend fun insertProgram(program: Program) {
        coachDao.insertProgram(program)
        FirestoreSync.saveProgram(program)
    }

    suspend fun updateProgram(program: Program) {
        coachDao.updateProgram(program)
        FirestoreSync.saveProgram(program)
    }

    suspend fun cloneProgram(program: Program) {
        val copy = program.copy(
            id = UUID.randomUUID().toString(),
            name = "${program.name} (Copy)",
            lastUsedMillis = System.currentTimeMillis(),
            clientCount = 0
        )
        coachDao.insertProgram(copy)
        FirestoreSync.saveProgram(copy)
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    suspend fun insertPayment(payment: Payment) {
        coachDao.insertPayment(payment)
        FirestoreSync.savePayment(payment)
    }

    // ─── Workout Logs ─────────────────────────────────────────────────────────

    suspend fun insertWorkoutLog(log: WorkoutLog) {
        coachDao.insertWorkoutLog(log)
        FirestoreSync.saveWorkoutLog(log)
    }

    // ─── Measurements ─────────────────────────────────────────────────────────

    suspend fun insertMeasurement(m: BodyMeasurement) {
        coachDao.insertMeasurement(m)
        FirestoreSync.saveMeasurement(m)
    }

    // ─── Notes ────────────────────────────────────────────────────────────────

    suspend fun insertNote(note: ClientNote) {
        coachDao.insertNote(note)
        FirestoreSync.saveNote(note)
    }

    suspend fun deleteNote(noteId: String) {
        coachDao.deleteNote(noteId)
        FirestoreSync.deleteNote(noteId)
    }

    // ─── Revenue Snapshots ────────────────────────────────────────────────────

    suspend fun saveRevenueSnapshot(totalMrr: Int) {
        val key = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val snap = RevenueSnapshot(key, totalMrr)
        coachDao.insertSnapshot(snap)
        FirestoreSync.saveSnapshot(snap)
    }

    // ─── Cloud Sync ───────────────────────────────────────────────────────────

    suspend fun syncFromFirestoreIfEmpty() {
        if (coachDao.getClientCount() == 0) {
            FirestoreSync.pullToRoom(coachDao)
        }
    }

    // ─── Logout: wipe all local data ──────────────────────────────────────────

    suspend fun clearAllLocalData() {
        coachDao.clearClients()
        coachDao.clearSignals()
        coachDao.clearPrograms()
        coachDao.clearPayments()
        coachDao.clearWorkoutLogs()
        coachDao.clearMeasurements()
        coachDao.clearNotes()
        coachDao.clearSnapshots()
    }

    // ─── Business logic ───────────────────────────────────────────────────────

    suspend fun advanceProgramWeeks(clients: List<Client>, programs: List<Program>) {
        val now = System.currentTimeMillis()
        for (client in clients) {
            if (client.programId == null || client.status != "Active") continue
            val program = programs.find { it.id == client.programId } ?: continue
            val daysSince = (now - client.enrollmentDateMillis) / 86400000L
            val computedWeek = (daysSince / 7).toInt() + 1
            val cappedWeek = computedWeek.coerceIn(1, program.durationWeeks)
            if (cappedWeek != client.programWeek) {
                val updated = client.copy(programWeek = cappedWeek)
                coachDao.updateClient(updated)
                FirestoreSync.saveClient(updated)
            }
        }
    }

    suspend fun recalculateConsistency(client: Client, program: Program?): Int {
        if (program == null) return client.consistencyScore
        val since = System.currentTimeMillis() - 28 * 86400000L
        val completedRecent = coachDao.getCompletedLogCountSince(client.id, since)
        val expectedInWindow = program.daysPerWeek * 4
        if (expectedInWindow == 0) return 100
        return ((completedRecent * 100) / expectedInWindow).coerceIn(0, 100)
    }

    suspend fun recomputeSignals(clients: List<Client>, payments: List<Payment>) {
        val now = System.currentTimeMillis()
        val newSignals = mutableListOf<Signal>()

        for (client in clients) {
            // Engagement signal: coach hasn't touched base in 10+ days
            val daysSinceCheckIn = (now - client.lastCheckInMillis) / 86400000L
            if (daysSinceCheckIn >= 10 && coachDao.hasUnresolvedSignalOfType(client.id, "FORM_CHECK") == 0)
                newSignals += Signal(UUID.randomUUID().toString(), client.id, client.name,
                    "FORM_CHECK", "No check-in for ${daysSinceCheckIn}d — time to touch base", "BLUE")

            if (client.programWeek > 0 && client.programWeek % 4 == 0 && coachDao.hasUnresolvedSignalOfType(client.id, "MILESTONE") == 0)
                newSignals += Signal(UUID.randomUUID().toString(), client.id, client.name,
                    "MILESTONE", "Week ${client.programWeek} complete — send a shoutout!", "BLUE")
        }

        for (payment in payments) {
            val client = clients.find { it.id == payment.clientId } ?: continue
            if (client.status == "Paused") continue  // no payment signals while client is on break
            when {
                payment.mandateStatus == "EXPIRING" && coachDao.hasUnresolvedSignalOfType(client.id, "UPI_EXPIRING") == 0 ->
                    newSignals += Signal(UUID.randomUUID().toString(), client.id, client.name,
                        "UPI_EXPIRING", "Auto-pay expiring soon — send renewal link", "RED")
                payment.mandateStatus == "FAILED" && coachDao.hasUnresolvedSignalOfType(client.id, "PAYMENT_OVERDUE") == 0 ->
                    newSignals += Signal(UUID.randomUUID().toString(), client.id, client.name,
                        "PAYMENT_OVERDUE", "₹${"%,d".format(payment.amount)} payment failed", "RED")
                payment.daysOverdue > 14 && payment.mandateStatus != "FAILED" && coachDao.hasUnresolvedSignalOfType(client.id, "PAYMENT_OVERDUE") == 0 ->
                    newSignals += Signal(UUID.randomUUID().toString(), client.id, client.name,
                        "PAYMENT_OVERDUE", "₹${"%,d".format(payment.amount)} is ${payment.daysOverdue}d overdue — follow up now", "RED")
                payment.daysOverdue in 7..14 && payment.mandateStatus != "FAILED" && coachDao.hasUnresolvedSignalOfType(client.id, "PAYMENT_OVERDUE") == 0 ->
                    newSignals += Signal(UUID.randomUUID().toString(), client.id, client.name,
                        "PAYMENT_OVERDUE", "₹${"%,d".format(payment.amount)} is ${payment.daysOverdue}d overdue", "YELLOW")
            }
        }

        if (newSignals.isNotEmpty()) coachDao.insertSignals(newSignals)
    }

    suspend fun getLowConsistencyClients() = coachDao.getLowConsistencyClients()
    suspend fun getOverduePayments()       = coachDao.getOverduePayments()
    suspend fun purgeLegacySignals()       = coachDao.deleteSignalsByType("MISSED_WORKOUT")

    companion object {
        @Volatile private var INSTANCE: CoachRepository? = null

        fun getInstance(context: Context): CoachRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CoachRepository(
                    AppDatabase.getInstance(context).coachDao()
                ).also { INSTANCE = it }
            }
    }
}

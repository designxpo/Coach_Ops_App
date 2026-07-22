package com.example.data

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide exercise library — fetched ONCE and shared by every FitnessViewModel.
 *
 * Previously each screen's ViewModel started its own Firestore listener over the
 * whole `exercises` collection. With 1,300+ imported exercises that meant every
 * screen re-downloaded the entire collection (slow + costly), and a detail screen
 * opened before its own listener returned couldn't find a freshly-imported
 * exercise → it sat on a loading spinner. One shared listener fixes both: the
 * list is downloaded a single time and reused, so detail lookups are instant.
 *
 * Seeds with the hardcoded library so the UI is never empty before the first
 * network round-trip.
 */
object ExerciseCatalog {

    private val _all = MutableStateFlow<List<Exercise>>(ExerciseRepository.all)
    val all: StateFlow<List<Exercise>> = _all.asStateFlow()

    private var listener: ListenerRegistration? = null

    /** Idempotent — the first caller starts the single shared listener. */
    fun start() {
        if (listener != null) return
        listener = FirestoreSync.listenExercises { merged -> _all.value = merged }
    }
}

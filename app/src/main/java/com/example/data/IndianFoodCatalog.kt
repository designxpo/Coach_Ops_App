package com.example.data

import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide Indian food library — the built-in [IndianFoodLibrary.all] merged with
 * admin edits from the Firestore `indian_foods` collection (one shared listener).
 * Admin can edit/unpublish the built-in dishes or add new ones; changes appear live.
 */
object IndianFoodCatalog {

    private val _all = MutableStateFlow(IndianFoodLibrary.all)
    val all: StateFlow<List<LibraryFood>> = _all.asStateFlow()

    private var listener: ListenerRegistration? = null

    /** Idempotent — first caller starts the single shared listener. */
    fun start() {
        if (listener != null) return
        listener = FirestoreSync.listenIndianFoods { merged -> _all.value = merged }
    }
}

package com.example.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val user: FirebaseUser, val isNewUser: Boolean = false) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private const val APP_VERSION  = "1.0.0"
    private const val WEB_CLIENT_ID =
        "566108244280-npl53hil8beahuj6i75hi0h80lgaatc0.apps.googleusercontent.com"

    val currentUser: FirebaseUser? get() = auth.currentUser

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user!!
            if (isUserSuspended(user.uid)) {
                auth.signOut()
                AuthResult.Error("Your account has been suspended. Contact support.")
            } else {
                writeUserRecord(user)
                AuthResult.Success(user, isNewUser = false)
            }
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e))
        }
    }

    suspend fun register(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user!!
            writeUserRecord(user)
            AuthResult.Success(user, isNewUser = true)
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e))
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!
            if (isUserSuspended(user.uid)) {
                auth.signOut()
                AuthResult.Error("Your account has been suspended. Contact support.")
            } else {
                val isNew = writeUserRecord(user)
                AuthResult.Success(user, isNewUser = isNew)
            }
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        auth.sendPasswordResetEmail(email.trim()).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun signOut(context: Context) {
        auth.signOut()
        getGoogleSignInClient(context).signOut()
    }

    fun signOut() = auth.signOut()

    fun handleGoogleSignInIntent(intent: Intent?) =
        GoogleSignIn.getSignedInAccountFromIntent(intent)

    private suspend fun isUserSuspended(uid: String): Boolean = try {
        val doc = db.collection("user_records").document(uid).get().await()
        doc.data?.get("suspended") as? Boolean ?: false
    } catch (_: Exception) { false }

    // Returns true if this is a genuinely new user (no prior Firestore record)
    private suspend fun writeUserRecord(user: FirebaseUser): Boolean {
        val docRef = db.collection("user_records").document(user.uid)
        val isNew = try { !docRef.get().await().exists() } catch (_: Exception) { false }
        val record = mutableMapOf<String, Any>(
            "uid"          to user.uid,
            "email"        to (user.email ?: ""),
            "displayName"  to (user.displayName ?: ""),
            "lastActiveAt" to System.currentTimeMillis(),
            "appVersion"   to APP_VERSION
        )
        if (isNew) {
            record["joinedAt"]  = System.currentTimeMillis()
            record["suspended"] = false
            record["plan"]      = "STARTER"
        }
        docRef.set(record, SetOptions.merge())
        return isNew
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is FirebaseAuthInvalidUserException        -> "No account found with this email"
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseAuthUserCollisionException      -> "An account with this email already exists"
        is FirebaseAuthWeakPasswordException       -> "Password must be at least 6 characters"
        else -> "Something went wrong. Check your connection and try again."
    }
}

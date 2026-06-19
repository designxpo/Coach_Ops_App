package com.example.data

/**
 * Supabase project credentials.
 * Project URL  → Supabase Dashboard → Settings → API → Project URL
 * Anon key     → Supabase Dashboard → Settings → API → anon public key
 *
 * The anon key is safe to include in the client — RLS policies control access.
 */
object SupabaseConfig {
    const val PROJECT_URL = "https://qscspcpnejbdebaskxgp.supabase.co"
    const val ANON_KEY    = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFzY3NwY3BuZWpiZGViYXNreGdwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk5NzY2NzYsImV4cCI6MjA5NTU1MjY3Nn0.JRYeK0EH5f3tqJQcFqpn5xi44kHlbSCDJIuFKDKBZyc"

    // Single public bucket — all app media lives here under sub-paths
    const val BUCKET = "profile-photos"

    // Path helpers
    fun userProfilePath(uid: String)          = "users/$uid"
    fun trainerProfilePath(uid: String)       = "trainer/$uid"
    fun portfolioPath(uid: String, idx: Int)  = "portfolio/$uid/$idx"
    fun exercisePath(exerciseId: String)      = "exercises/$exerciseId"

    fun publicUrl(path: String) =
        "$PROJECT_URL/storage/v1/object/public/$BUCKET/$path"
}

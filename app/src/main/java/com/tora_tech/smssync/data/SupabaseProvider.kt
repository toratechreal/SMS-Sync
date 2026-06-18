package com.tora_tech.smssync.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Builds and caches a single [SupabaseClient] for the currently paired project.
 * Re-builds only when the url/key change.
 */
object SupabaseProvider {

    @Volatile private var client: SupabaseClient? = null
    @Volatile private var signature: String? = null

    @Synchronized
    fun get(url: String, anonKey: String): SupabaseClient {
        val sig = "$url|$anonKey"
        client?.let { if (signature == sig) return it }
        val created = createSupabaseClient(supabaseUrl = url, supabaseKey = anonKey) {
            install(Postgrest)
            install(Realtime)
        }
        client = created
        signature = sig
        return created
    }

    /** Convenience: build from a paired [AppConfig], or null if not paired. */
    fun from(config: AppConfig): SupabaseClient? {
        if (!config.isPaired) return null
        return get(config.supabaseUrl!!, config.supabaseKey!!)
    }
}

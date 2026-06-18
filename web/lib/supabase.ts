import { createClient, SupabaseClient } from "@supabase/supabase-js"
import { SupabaseConfig } from "./config"

let cached: SupabaseClient | null = null
let signature: string | null = null

/** Builds (and caches) a Supabase client for the given config. */
export function getSupabase(config: SupabaseConfig): SupabaseClient {
  const sig = `${config.url}|${config.key}`
  if (cached && signature === sig) return cached
  cached = createClient(config.url, config.key)
  signature = sig
  return cached
}

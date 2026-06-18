export interface SupabaseConfig {
  url: string
  key: string
}

/**
 * Reads Supabase config from environment variables, provisioned by the
 * Vercel ↔ Supabase integration (or set manually in Vercel project settings).
 * NEXT_PUBLIC_* vars are inlined at build time and available in the browser.
 *
 * Accepts both the new "publishable" key (sb_publishable_…) and the legacy
 * "anon" JWT key, whichever is present.
 *
 * For local dev, put them in web/.env.local, e.g.:
 *   NEXT_PUBLIC_SUPABASE_URL=...
 *   NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
 */
export function loadConfig(): SupabaseConfig | null {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL
  const key =
    process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY ??
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY
  if (url && key) return { url, key }
  return null
}

export function isConfigured(): boolean {
  return loadConfig() !== null
}

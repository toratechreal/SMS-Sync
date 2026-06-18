import Link from "next/link"

/** Shown when the Supabase env vars aren't set. */
export function NotConfigured() {
  return (
    <div className="rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm dark:border-amber-700/50 dark:bg-amber-950/30">
      <p className="font-medium">Not connected to Supabase yet.</p>
      <p className="mt-1 text-neutral-600 dark:text-neutral-400">
        This deployment reads <code>NEXT_PUBLIC_SUPABASE_URL</code> and{" "}
        <code>NEXT_PUBLIC_SUPABASE_ANON_KEY</code> from environment variables. Connect
        the Vercel ↔ Supabase integration (or set these in Vercel project settings),
        then redeploy.
      </p>
      <p className="mt-2">
        <Link href="/settings" className="font-medium underline">
          See connection status →
        </Link>
      </p>
    </div>
  )
}

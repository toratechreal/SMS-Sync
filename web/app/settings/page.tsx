"use client"

import { loadConfig } from "@/lib/config"
import { PagePane } from "@/components/PagePane"

export default function ConnectionPage() {
  const cfg = loadConfig()

  return (
    <PagePane title="Connection">
      <div className="flex flex-col gap-4">
      {cfg ? (
        <div className="rounded-xl border border-green-300 bg-green-50 p-4 text-sm dark:border-green-700/50 dark:bg-green-950/30">
          <p className="font-medium">Connected to Supabase.</p>
          <p className="mt-1 break-all text-neutral-600 dark:text-neutral-400">
            URL: <code>{cfg.url}</code>
          </p>
          <p className="text-neutral-600 dark:text-neutral-400">
            Anon key: <code>{maskKey(cfg.key)}</code>
          </p>
        </div>
      ) : (
        <div className="rounded-xl border border-amber-300 bg-amber-50 p-4 text-sm dark:border-amber-700/50 dark:bg-amber-950/30">
          <p className="font-medium">Not connected.</p>
          <p className="mt-1 text-neutral-600 dark:text-neutral-400">
            No Supabase environment variables detected.
          </p>
        </div>
      )}

      <div className="text-sm text-neutral-600 dark:text-neutral-400">
        <p className="font-medium text-neutral-900 dark:text-neutral-100">
          How config works
        </p>
        <p className="mt-1">
          This app reads its Supabase connection from environment variables, set
          automatically by the Vercel ↔ Supabase integration:
        </p>
        <ul className="mt-2 list-disc space-y-1 pl-5">
          <li>
            <code>NEXT_PUBLIC_SUPABASE_URL</code>
          </li>
          <li>
            <code>NEXT_PUBLIC_SUPABASE_ANON_KEY</code>
          </li>
        </ul>
        <p className="mt-2">
          In Vercel: open the project → <strong>Integrations</strong> → add{" "}
          <strong>Supabase</strong> → connect your project. It provisions these vars;
          then <strong>redeploy</strong> so the new build picks them up. (NEXT_PUBLIC
          vars are baked in at build time.)
        </p>
        <p className="mt-2">
          For local dev, put them in <code>web/.env.local</code>.
        </p>
      </div>
      </div>
    </PagePane>
  )
}

function maskKey(key: string): string {
  if (key.length <= 12) return "••••"
  return `${key.slice(0, 6)}…${key.slice(-4)}`
}

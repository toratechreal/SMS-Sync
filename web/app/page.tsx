"use client"

import { isConfigured } from "@/lib/config"
import { NotConfigured } from "@/components/NotConfigured"

export default function HomePage() {
  if (!isConfigured()) {
    return (
      <div className="flex h-full w-full flex-1 items-center justify-center p-6">
        <div className="max-w-md">
          <NotConfigured />
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full w-full flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
      <svg width="220" height="150" viewBox="0 0 220 150" fill="none" aria-hidden>
        <rect x="118" y="20" width="90" height="60" rx="12" fill="#e8f0fe" />
        <rect x="132" y="36" width="48" height="9" rx="4.5" fill="#4285F4" />
        <rect x="132" y="54" width="34" height="9" rx="4.5" fill="#4285F4" />
        <circle cx="190" cy="58" r="5" fill="#fff" stroke="#4285F4" strokeWidth="2" />
        <rect x="40" y="56" width="120" height="74" rx="14" fill="#4285F4" />
        <circle cx="62" cy="76" r="7" fill="#fff" />
        <rect x="76" y="71" width="60" height="9" rx="4.5" fill="#fff" />
        <rect x="62" y="98" width="78" height="9" rx="4.5" fill="#fff" />
        <circle cx="64" cy="120" r="6" fill="#fff" />
        <circle cx="120" cy="120" r="5" fill="#fff" />
      </svg>
      <p className="text-sm text-neutral-500 dark:text-neutral-400">
        Select a conversation to start messaging.
      </p>
    </div>
  )
}

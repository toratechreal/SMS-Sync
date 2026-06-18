"use client"

import { useMemo, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { useContacts } from "@/lib/useContacts"
import { Avatar } from "@/components/Avatar"

export default function NewChatPage() {
  const router = useRouter()
  const [query, setQuery] = useState("")
  const contacts = useContacts()

  const open = (address: string) => {
    const a = address.trim()
    if (a) router.push(`/thread/${encodeURIComponent(a)}`)
  }

  const q = query.trim()
  const qDigits = q.replace(/\D/g, "")
  const all = useMemo(
    () => Array.from(contacts.entries()).map(([phone, name]) => ({ phone, name })),
    [contacts],
  )
  const filtered = useMemo(() => {
    if (!q) return all
    return all.filter(
      (c) =>
        c.name.toLowerCase().includes(q.toLowerCase()) ||
        (qDigits && c.phone.includes(qDigits)),
    )
  }, [all, q, qDigits])

  const showSendTo =
    qDigits.length >= 3 && !filtered.some((c) => c.phone === qDigits)

  return (
    <div className="flex h-full w-full flex-col bg-white dark:bg-neutral-950">
      <header className="flex items-center gap-3 border-b border-neutral-200 px-4 py-3 dark:border-neutral-800">
        <Link
          href="/"
          className="-ml-1 rounded-full p-1.5 text-neutral-600 hover:bg-neutral-100 md:hidden dark:text-neutral-300 dark:hover:bg-neutral-800"
          aria-label="Back"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20z" />
          </svg>
        </Link>
        <h1 className="text-base font-medium text-neutral-900 dark:text-neutral-100">
          New conversation
        </h1>
      </header>

      <div className="border-b border-neutral-200 p-3 dark:border-neutral-800">
        <input
          type="tel"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && q) {
              e.preventDefault()
              open(q)
            }
          }}
          placeholder="Type a name or number"
          autoFocus
          className="w-full rounded-xl border border-neutral-300 bg-neutral-50 px-4 py-3 text-[15px] outline-none focus:border-[#1a73e8] dark:border-neutral-700 dark:bg-neutral-900"
        />
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {showSendTo && (
          <Row name={`Send to ${q}`} subtitle={null} avatarKey={q} onClick={() => open(q)} />
        )}
        {filtered.map((c) => (
          <Row
            key={c.phone}
            name={c.name}
            subtitle={c.phone}
            avatarKey={c.name}
            onClick={() => open(c.phone)}
          />
        ))}
        {!showSendTo && filtered.length === 0 && (
          <p className="px-4 py-6 text-sm text-neutral-500">
            No contacts. Type a full number and press Enter to start.
          </p>
        )}
      </div>
    </div>
  )
}

function Row({
  name,
  subtitle,
  avatarKey,
  onClick,
}: {
  name: string
  subtitle: string | null
  avatarKey: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="flex w-full items-center gap-3 px-4 py-2.5 text-left transition hover:bg-neutral-100 dark:hover:bg-neutral-800"
    >
      <Avatar address={avatarKey} />
      <div className="min-w-0 flex-1">
        <div className="truncate text-[15px] text-neutral-900 dark:text-neutral-100">
          {name}
        </div>
        {subtitle && (
          <div className="truncate text-sm text-neutral-500 dark:text-neutral-400">
            {subtitle}
          </div>
        )}
      </div>
    </button>
  )
}

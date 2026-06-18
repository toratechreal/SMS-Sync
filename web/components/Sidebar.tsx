"use client"

import { useMemo } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { isConfigured } from "@/lib/config"
import { conversations, useMessages } from "@/lib/useMessages"
import { normalizeAddress } from "@/lib/normalize"
import { shortTime } from "@/lib/display"
import { useContacts } from "@/lib/useContacts"
import { useOutgoing } from "@/lib/useOutgoing"
import { StatusChip } from "./StatusChip"
import { Avatar } from "./Avatar"

export function Sidebar() {
  const configured = isConfigured()
  const { messages, ready } = useMessages()
  const outgoing = useOutgoing()
  const contacts = useContacts()
  const threads = useMemo(
    () => conversations(messages, outgoing),
    [messages, outgoing],
  )

  const params = useParams<{ address?: string }>()
  const activeKey = params?.address
    ? normalizeAddress(
        decodeURIComponent(
          Array.isArray(params.address) ? params.address[0] : params.address,
        ),
      )
    : null

  return (
    <aside className="flex h-full w-full flex-col border-r border-neutral-200 bg-white dark:border-neutral-800 dark:bg-neutral-900 md:w-[380px]">
      {/* Brand bar */}
      <div className="flex items-center gap-3 px-5 py-4">
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[#1a73e8] text-white">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z" />
          </svg>
        </span>
        <span className="text-[20px] font-medium tracking-tight text-neutral-800 dark:text-neutral-100">
          SMS Sync
        </span>
      </div>

      {/* Start chat */}
      <div className="px-4 pb-2">
        <Link
          href="/new"
          className="inline-flex items-center gap-2 rounded-full bg-[#c2e7ff] px-5 py-3 text-sm font-medium text-[#001d35] transition hover:shadow-md dark:bg-[#004a77] dark:text-[#c2e7ff]"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
            <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 9h-2v2h-2v-2H7V9h2V7h2v2h2v2z" />
          </svg>
          Start chat
        </Link>
      </div>

      {/* Conversation list */}
      <nav className="min-h-0 flex-1 overflow-y-auto px-2 pb-3">
        {ready && threads.length === 0 && (
          <p className="px-3 py-6 text-sm text-neutral-500">
            {configured ? "No messages yet." : "Not connected."}
          </p>
        )}
        {threads.map((c) => {
          const active = normalizeAddress(c.address) === activeKey
          const title = contacts.get(normalizeAddress(c.address)) ?? c.address
          return (
            <Link
              key={c.address}
              href={`/thread/${encodeURIComponent(c.address)}`}
              className={
                "flex items-center gap-3 rounded-2xl px-3 py-2.5 transition " +
                (active
                  ? "bg-[#c2e7ff]/60 dark:bg-[#004a77]/50"
                  : "hover:bg-neutral-100 dark:hover:bg-neutral-800")
              }
            >
              <Avatar address={title} />
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline justify-between gap-2">
                  <span className="truncate text-[15px] font-medium text-neutral-900 dark:text-neutral-100">
                    {title}
                  </span>
                  <time className="shrink-0 text-xs text-neutral-500">
                    {shortTime(c.lastTimestamp)}
                  </time>
                </div>
                <div className="flex items-center gap-1 truncate text-sm text-neutral-500 dark:text-neutral-400">
                  {c.lastStatus && (
                    <>
                      <StatusChip status={c.lastStatus} />
                      <span>·</span>
                    </>
                  )}
                  <span className="truncate">{c.lastBody}</span>
                </div>
              </div>
            </Link>
          )
        })}
      </nav>

      {/* Footer links */}
      <div className="flex items-center gap-4 border-t border-neutral-200 px-5 py-3 text-xs text-neutral-500 dark:border-neutral-800">
        <Link href="/settings" className="hover:text-neutral-800 dark:hover:text-neutral-200">
          Connection
        </Link>
        <Link href="/status" className="hover:text-neutral-800 dark:hover:text-neutral-200">
          Status
        </Link>
      </div>
    </aside>
  )
}

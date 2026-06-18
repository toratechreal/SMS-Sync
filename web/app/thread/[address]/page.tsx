"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { loadConfig } from "@/lib/config"
import { getSupabase } from "@/lib/supabase"
import { thread, useMessages } from "@/lib/useMessages"
import { useOutgoing } from "@/lib/useOutgoing"
import { useContacts } from "@/lib/useContacts"
import { normalizeAddress } from "@/lib/normalize"
import { ThreadItem } from "@/lib/types"
import { bubbleTime, dayLabel } from "@/lib/display"
import { Avatar } from "@/components/Avatar"

export default function ThreadPage() {
  const params = useParams<{ address: string }>()
  const address = decodeURIComponent(
    Array.isArray(params.address) ? params.address[0] : params.address,
  )
  const { messages } = useMessages()
  const outgoing = useOutgoing()
  const contacts = useContacts()
  const name = contacts.get(normalizeAddress(address))
  const items = useMemo(
    () => thread(messages, outgoing, address),
    [messages, outgoing, address],
  )

  const [draft, setDraft] = useState("")
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const scrollRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight })
  }, [items.length])

  const send = async () => {
    const body = draft.trim()
    if (!body) return
    const cfg = loadConfig()
    if (!cfg) return
    setSending(true)
    setError(null)
    try {
      const { error } = await getSupabase(cfg)
        .from("outgoing")
        .insert({ address, body, status: "pending" })
      if (error) setError(error.message)
      else setDraft("")
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to send")
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="flex h-full w-full flex-col bg-white dark:bg-neutral-950">
      {/* Header */}
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
        <Avatar address={name ?? address} size={36} />
        <div className="min-w-0">
          <h1 className="truncate text-base font-medium text-neutral-900 dark:text-neutral-100">
            {name ?? address}
          </h1>
          {name && <p className="truncate text-xs text-neutral-500">{address}</p>}
        </div>
      </header>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4">
        <div className="mx-auto flex max-w-3xl flex-col gap-1">
          {items.map((m, i) => {
            const prev = items[i - 1]
            const showDay = !prev || !sameDay(prev.timestamp, m.timestamp)
            return (
              <div key={m.key}>
                {showDay && (
                  <div className="py-3 text-center text-xs font-medium text-neutral-500">
                    {dayLabel(m.timestamp)}
                  </div>
                )}
                <Bubble item={m} grouped={!showDay && prev?.outbound === m.outbound} />
              </div>
            )
          })}
        </div>
      </div>

      {error && (
        <p className="px-4 pb-1 text-center text-sm text-red-600">{error}</p>
      )}

      {/* Composer */}
      <div className="px-4 pb-4 pt-1">
        <div className="mx-auto flex max-w-3xl items-end gap-2">
          <div className="flex flex-1 items-end rounded-3xl border border-neutral-300 bg-neutral-50 px-4 py-2 dark:border-neutral-700 dark:bg-neutral-900">
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="Text message"
              rows={1}
              className="max-h-32 flex-1 resize-none bg-transparent py-1 text-[15px] outline-none placeholder:text-neutral-500"
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault()
                  send()
                }
              }}
            />
          </div>
          <button
            onClick={send}
            disabled={sending || !draft.trim()}
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#1a73e8] text-white transition hover:bg-[#1765cc] disabled:bg-neutral-300 disabled:text-neutral-500 dark:disabled:bg-neutral-700"
            aria-label="Send"
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  )
}

function sameDay(a: number, b: number): boolean {
  const da = new Date(a)
  const db = new Date(b)
  return (
    da.getFullYear() === db.getFullYear() &&
    da.getMonth() === db.getMonth() &&
    da.getDate() === db.getDate()
  )
}

function Bubble({ item, grouped }: { item: ThreadItem; grouped: boolean }) {
  const outbound = item.outbound
  return (
    <div className={(outbound ? "justify-end" : "justify-start") + " group flex"}>
      <div
        className={
          "max-w-[75%] px-4 py-2 text-[15px] leading-relaxed " +
          (outbound
            ? "rounded-3xl bg-[#1a73e8] text-white"
            : "rounded-3xl bg-neutral-200 text-neutral-900 dark:bg-neutral-800 dark:text-neutral-100") +
          (grouped ? (outbound ? " rounded-tr-md" : " rounded-tl-md") : "")
        }
        title={new Date(item.timestamp).toLocaleString()}
      >
        <div className="whitespace-pre-wrap break-words">{item.body}</div>
        <div
          className={
            "mt-0.5 flex items-center gap-2 text-[11px] " +
            (outbound ? "text-blue-100" : "text-neutral-500")
          }
        >
          <span>{bubbleTime(item.timestamp)}</span>
          {outbound && item.status && (
            <span className={item.status === "failed" ? "text-red-200" : ""}>
              {item.status === "outbox"
                ? "· Outbox"
                : item.status === "failed"
                  ? "· Failed"
                  : "· Sent"}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}

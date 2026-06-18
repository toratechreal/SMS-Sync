"use client"

import { useEffect, useState } from "react"
import { loadConfig } from "./config"
import { getSupabase } from "./supabase"
import { normalizeAddress } from "./normalize"
import { Conversation, MessageRow, OutgoingRow, ThreadItem } from "./types"

/** Streams all messages: initial fetch + re-fetch on any realtime change. */
export function useMessages(): { messages: MessageRow[]; ready: boolean } {
  const [messages, setMessages] = useState<MessageRow[]>([])
  const [ready, setReady] = useState(false)

  useEffect(() => {
    const cfg = loadConfig()
    if (!cfg) {
      setReady(true)
      return
    }
    const sb = getSupabase(cfg)
    let active = true

    const fetchAll = async () => {
      const { data } = await sb
        .from("messages")
        .select("*")
        .order("sms_timestamp", { ascending: true })
      if (active) {
        setMessages((data as MessageRow[]) ?? [])
        setReady(true)
      }
    }

    fetchAll()
    const channel = sb
      .channel(`web-messages-${Math.random().toString(36).slice(2)}`)
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "messages" },
        () => {
          fetchAll()
        },
      )
      .subscribe()

    return () => {
      active = false
      sb.removeChannel(channel)
    }
  }, [])

  return { messages, ready }
}

/** Merge messages + un-sent queue into one time-sorted list with statuses. */
export function buildItems(
  messages: MessageRow[],
  outgoing: OutgoingRow[],
): ThreadItem[] {
  const items: ThreadItem[] = []
  for (const m of messages) {
    const outbound = m.direction === "outbound"
    items.push({
      key: "m:" + (m.id ?? m.dedupe_key),
      address: m.address,
      body: m.body,
      outbound,
      timestamp: m.sms_timestamp,
      status: outbound ? "sent" : null,
    })
  }
  for (const o of outgoing) {
    items.push({
      key: "o:" + (o.id ?? o.address + o.body),
      address: o.address,
      body: o.body,
      outbound: true,
      timestamp: o.created_at ? Date.parse(o.created_at) : Date.now(),
      status: o.status === "failed" ? "failed" : "outbox",
    })
  }
  return items.sort((a, b) => a.timestamp - b.timestamp)
}

/** Distinct threads (grouped by digits-only number), most recent first. */
export function conversations(
  messages: MessageRow[],
  outgoing: OutgoingRow[],
): Conversation[] {
  const items = buildItems(messages, outgoing)
  const groups = new Map<string, ThreadItem[]>()
  for (const it of items) {
    const key = normalizeAddress(it.address)
    const arr = groups.get(key) ?? []
    arr.push(it)
    groups.set(key, arr)
  }
  const result: Conversation[] = []
  for (const arr of groups.values()) {
    const last = arr.reduce((a, b) => (b.timestamp > a.timestamp ? b : a))
    result.push({
      address: last.address,
      lastBody: last.body,
      lastTimestamp: last.timestamp,
      lastStatus: last.status,
    })
  }
  return result.sort((a, b) => b.lastTimestamp - a.lastTimestamp)
}

/** Thread items (matched by normalized address), oldest first. */
export function thread(
  messages: MessageRow[],
  outgoing: OutgoingRow[],
  address: string,
): ThreadItem[] {
  const key = normalizeAddress(address)
  return buildItems(messages, outgoing).filter(
    (it) => normalizeAddress(it.address) === key,
  )
}

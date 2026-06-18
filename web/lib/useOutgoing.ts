"use client"

import { useEffect, useState } from "react"
import { loadConfig } from "./config"
import { getSupabase } from "./supabase"
import { OutgoingRow } from "./types"

/** Streams not-yet-sent queue items (pending + failed) for status chips, live. */
export function useOutgoing(): OutgoingRow[] {
  const [rows, setRows] = useState<OutgoingRow[]>([])

  useEffect(() => {
    const cfg = loadConfig()
    if (!cfg) return
    const sb = getSupabase(cfg)
    let active = true

    const fetchUnsent = async () => {
      const { data } = await sb
        .from("outgoing")
        .select("*")
        .neq("status", "sent")
        .order("created_at", { ascending: true })
      if (active) setRows((data as OutgoingRow[]) ?? [])
    }

    fetchUnsent()
    const channel = sb
      .channel(`web-outgoing-${Math.random().toString(36).slice(2)}`)
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "outgoing" },
        () => {
          fetchUnsent()
        },
      )
      .subscribe()

    return () => {
      active = false
      sb.removeChannel(channel)
    }
  }, [])

  return rows
}

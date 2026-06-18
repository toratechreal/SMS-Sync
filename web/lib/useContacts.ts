"use client"

import { useEffect, useState } from "react"
import { loadConfig } from "./config"
import { getSupabase } from "./supabase"
import { ContactRow } from "./types"

/** Streams the contacts table as a map of normalized phone -> name, live. */
export function useContacts(): Map<string, string> {
  const [map, setMap] = useState<Map<string, string>>(new Map())

  useEffect(() => {
    const cfg = loadConfig()
    if (!cfg) return
    const sb = getSupabase(cfg)
    let active = true

    const fetchAll = async () => {
      const { data } = await sb.from("contacts").select("*")
      if (active && data) {
        setMap(new Map((data as ContactRow[]).map((c) => [c.phone, c.name])))
      }
    }

    fetchAll()
    const channel = sb
      .channel(`web-contacts-${Math.random().toString(36).slice(2)}`)
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "contacts" },
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

  return map
}

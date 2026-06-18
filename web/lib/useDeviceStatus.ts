"use client"

import { useCallback, useEffect, useState } from "react"
import { loadConfig } from "./config"
import { getSupabase } from "./supabase"
import { pingRefresh } from "./ping"
import { DeviceStatusRow } from "./types"

const sortHostFirst = (rows: DeviceStatusRow[]) =>
  [...rows].sort((a, b) => (a.role === "host" ? -1 : 0) - (b.role === "host" ? -1 : 0))

/** Streams device_status rows (host first), live, with a manual refresh. */
export function useDeviceStatus(): {
  statuses: DeviceStatusRow[]
  ready: boolean
  refresh: () => Promise<void>
  remove: (deviceId: string) => Promise<void>
} {
  const [statuses, setStatuses] = useState<DeviceStatusRow[]>([])
  const [ready, setReady] = useState(false)

  const refresh = useCallback(async () => {
    const cfg = loadConfig()
    if (!cfg) return
    await pingRefresh()
    const { data } = await getSupabase(cfg).from("device_status").select("*")
    setStatuses(sortHostFirst((data as DeviceStatusRow[]) ?? []))
  }, [])

  const remove = useCallback(async (deviceId: string) => {
    const cfg = loadConfig()
    if (!cfg) return
    const sb = getSupabase(cfg)
    await sb.from("device_status").delete().eq("device_id", deviceId)
    const { data } = await sb.from("device_status").select("*")
    setStatuses(sortHostFirst((data as DeviceStatusRow[]) ?? []))
  }, [])

  useEffect(() => {
    const cfg = loadConfig()
    if (!cfg) {
      setReady(true)
      return
    }
    const sb = getSupabase(cfg)
    let active = true

    const fetchAll = async () => {
      const { data } = await sb.from("device_status").select("*")
      if (active) {
        setStatuses(sortHostFirst((data as DeviceStatusRow[]) ?? []))
        setReady(true)
      }
    }

    fetchAll()
    const channel = sb
      .channel(`web-status-${Math.random().toString(36).slice(2)}`)
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: "device_status" },
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

  return { statuses, ready, refresh, remove }
}

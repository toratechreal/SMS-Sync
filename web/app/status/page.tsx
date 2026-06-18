"use client"

import { useState } from "react"
import { useDeviceStatus } from "@/lib/useDeviceStatus"
import { DeviceStatusRow } from "@/lib/types"
import { PagePane } from "@/components/PagePane"
import { isOnline, relativeTime } from "@/lib/display"

export default function StatusPage() {
  const { statuses, ready, refresh, remove } = useDeviceStatus()
  const [refreshing, setRefreshing] = useState(false)

  const onRefresh = async () => {
    setRefreshing(true)
    try {
      await refresh()
    } finally {
      setRefreshing(false)
    }
  }

  return (
    <PagePane title="Device status">
      <div className="flex flex-col gap-3">
        <button
          onClick={onRefresh}
          disabled={refreshing}
          className="self-end rounded-full border border-neutral-300 px-4 py-1.5 text-sm font-medium text-neutral-700 transition hover:bg-neutral-100 disabled:opacity-50 dark:border-neutral-700 dark:text-neutral-200 dark:hover:bg-neutral-800"
        >
          {refreshing ? "Refreshing…" : "Refresh"}
        </button>
        {ready && statuses.length === 0 && (
          <p className="text-sm text-neutral-500">No device status yet.</p>
        )}
        {statuses.map((s) => (
          <StatusCard key={s.device_id} status={s} onRemove={() => remove(s.device_id)} />
        ))}
      </div>
    </PagePane>
  )
}

function StatusCard({
  status,
  onRemove,
}: {
  status: DeviceStatusRow
  onRemove: () => void
}) {
  const online = isOnline(status.last_seen)
  const role = status.role
    ? status.role.charAt(0).toUpperCase() + status.role.slice(1)
    : "Device"

  return (
    <div className="rounded-2xl border border-neutral-200 p-4 dark:border-neutral-800">
      <div className="flex items-center justify-between">
        <span className="text-[15px] font-medium text-neutral-900 dark:text-neutral-100">
          {role}
        </span>
        <span className="flex items-center gap-1.5 text-xs text-neutral-500">
          <span
            className={
              "h-2 w-2 rounded-full " +
              (online ? "bg-green-500" : "bg-neutral-400")
            }
          />
          {online ? "Online" : relativeTime(status.last_seen)}
        </span>
      </div>

      <dl className="mt-3 space-y-1.5 text-sm">
        {status.phone_number && <Row label="Number" value={status.phone_number} />}
        {status.carrier && <Row label="Carrier" value={status.carrier} />}
        {status.signal_strength != null && (
          <Row label="Signal" value={`${status.signal_strength}/4`} />
        )}
        {status.battery_level != null && (
          <Row
            label="Battery"
            value={`${status.battery_level}%${status.battery_charging ? " · Charging" : ""}`}
          />
        )}
      </dl>

      <div className="mt-3 flex justify-end">
        <button
          onClick={onRemove}
          className="text-xs font-medium text-red-600 hover:underline dark:text-red-400"
        >
          Remove
        </button>
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-3">
      <dt className="w-20 shrink-0 text-neutral-500 dark:text-neutral-400">{label}</dt>
      <dd className="text-neutral-900 dark:text-neutral-100">{value}</dd>
    </div>
  )
}

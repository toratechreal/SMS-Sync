import { MsgStatus } from "@/lib/types"

/** Tiny send-status label for outbound items. */
export function StatusChip({ status }: { status: MsgStatus }) {
  const label = status === "outbox" ? "Outbox" : status === "failed" ? "Failed" : "Sent"
  const cls =
    status === "failed"
      ? "text-red-600 dark:text-red-400"
      : "text-neutral-500 dark:text-neutral-400"
  return <span className={"text-[11px] " + cls}>{label}</span>
}

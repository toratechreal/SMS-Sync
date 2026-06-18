/** Presentation helpers shared by the conversation list and thread views. */

/** Initial shown inside the circular avatar. */
export function initial(address: string): string {
  const ch = address.trim().charAt(0)
  return /[a-z0-9]/i.test(ch) ? ch.toUpperCase() : "#"
}

/** Stable per-contact avatar color, matching Google Messages' palette feel. */
const AVATAR_COLORS = [
  "#1a73e8", // blue
  "#d93025", // red
  "#188038", // green
  "#e37400", // amber
  "#9334e6", // purple
  "#129eaf", // teal
  "#c5221f", // crimson
  "#a8a116", // olive
]

export function avatarColor(address: string): string {
  let hash = 0
  for (let i = 0; i < address.length; i++) {
    hash = (hash * 31 + address.charCodeAt(i)) >>> 0
  }
  return AVATAR_COLORS[hash % AVATAR_COLORS.length]
}

/** Short timestamp for the conversation list (time today, weekday this week, else date). */
export function shortTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  const sameDay =
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  if (sameDay) {
    return d.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })
  }
  const ageDays = (now.getTime() - d.getTime()) / 86_400_000
  if (ageDays < 7) return d.toLocaleDateString([], { weekday: "short" })
  return d.toLocaleDateString([], { month: "short", day: "numeric" })
}

/** Bubble time, e.g. "2:31 AM". */
export function bubbleTime(ts: number): string {
  return new Date(ts).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })
}

/** Relative "last seen" from an ISO timestamp: just now / 5m ago / 2h ago / 3d ago / date. */
export function relativeTime(iso: string | null | undefined): string {
  if (!iso) return "—"
  const ms = Date.parse(iso)
  if (Number.isNaN(ms)) return "—"
  const diff = Date.now() - ms
  if (diff < 60_000) return "just now"
  const mins = Math.floor(diff / 60_000)
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return new Date(ms).toLocaleDateString([], { month: "short", day: "numeric" })
}

/** True if the device reported within ~3 minutes. */
export function isOnline(iso: string | null | undefined): boolean {
  if (!iso) return false
  const ms = Date.parse(iso)
  return !Number.isNaN(ms) && Date.now() - ms < 3 * 60_000
}

/** Day separator label, e.g. "Monday" or "Jun 11". */
export function dayLabel(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  const ageDays = (now.getTime() - d.getTime()) / 86_400_000
  if (ageDays < 7) return d.toLocaleDateString([], { weekday: "long" })
  return d.toLocaleDateString([], { month: "short", day: "numeric" })
}

import { loadConfig } from "./config"
import { getSupabase } from "./supabase"

/**
 * Broadcasts a "ping to refresh" on the shared device-ping channel so connected
 * devices report their telemetry immediately. Ephemeral (no DB); only reaches
 * devices whose service is currently connected.
 */
export async function pingRefresh(): Promise<void> {
  const cfg = loadConfig()
  if (!cfg) return
  const sb = getSupabase(cfg)
  const ch = sb.channel("device-ping")
  await new Promise<void>((resolve) => {
    let done = false
    const finish = () => {
      if (done) return
      done = true
      sb.removeChannel(ch)
      resolve()
    }
    ch.subscribe((status) => {
      if (status === "SUBSCRIBED") {
        ch.send({ type: "broadcast", event: "refresh", payload: {} }).finally(finish)
      } else if (
        status === "CHANNEL_ERROR" ||
        status === "TIMED_OUT" ||
        status === "CLOSED"
      ) {
        finish()
      }
    })
    setTimeout(finish, 3000)
  })
}

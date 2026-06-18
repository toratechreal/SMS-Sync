export interface MessageRow {
  id?: string
  address: string
  body: string
  direction: "inbound" | "outbound"
  sms_timestamp: number
  dedupe_key: string
  read?: boolean
  created_at?: string
}

export interface OutgoingRow {
  id?: string
  address: string
  body: string
  status?: "pending" | "sent" | "failed"
  error?: string | null
  created_at?: string
  sent_at?: string | null
}

export interface DeviceStatusRow {
  device_id: string
  role: string
  phone_number?: string | null
  carrier?: string | null
  signal_strength?: number | null
  battery_level?: number | null
  battery_charging?: boolean | null
  app_version?: string | null
  last_seen?: string | null
}

export interface ContactRow {
  phone: string
  name: string
  source_device_id?: string | null
  updated_at?: string | null
}

export type MsgStatus = "outbox" | "sent" | "failed"

export interface ThreadItem {
  key: string
  address: string
  body: string
  outbound: boolean
  timestamp: number
  status: MsgStatus | null
}

export interface Conversation {
  address: string
  lastBody: string
  lastTimestamp: number
  lastStatus: MsgStatus | null
}

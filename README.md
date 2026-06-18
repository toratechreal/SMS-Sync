# SMS-Sync

Read and reply to your phone's texts from other devices and the web — using **your own** Supabase backend.

## How it works

One Android app, two modes, plus a web client:

- **Host** — the phone with the SIM. Captures incoming SMS, backfills history, and sends your replies.
- **Client** — any other Android device. Reads synced messages and replies.
- **Web** — a Next.js app (deploy to Vercel) that does the same in the browser.

Everything syncs through a Supabase project you own. Pair devices by QR code.

## Project structure

```
SMS-Sync/
├─ app/                         # Android app (Kotlin + Jetpack Compose)
│  └─ src/main/.../smssync/
│     ├─ host/                  # Host service: SMS send + queue drain
│     ├─ client/                # Client service
│     ├─ sms/                   # SMS receiver + sender
│     ├─ contacts/              # Contact-name sync
│     ├─ device/                # Telemetry, boot, watchdog, keep-alive
│     ├─ pairing/               # QR pairing
│     ├─ data/                  # Supabase repositories + config
│     └─ ui/                    # Compose screens (client/, host/, theme/)
├─ web/                         # Next.js web client (deploy to Vercel)
│  ├─ app/                      # Routes: conversations, thread, new, settings, status
│  ├─ components/               # UI components
│  └─ lib/                      # Supabase hooks + helpers
├─ supabase/
│  └─ schema.sql                # Database schema — run this in your project
├─ tools/
│  └─ pairing-qr-generator.html # Build a pairing QR from your Supabase keys
└─ README.md
```

## Setup

### 1. Supabase
1. Create a free project at [supabase.com](https://supabase.com).
2. Open the **SQL Editor** and run [`supabase/schema.sql`](supabase/schema.sql) (creates the tables + realtime).
3. In **Settings → API**, copy your **Project URL** and **anon (publishable) key** — you'll need them to pair.

### 2. Install the Android app
Download the latest APK from [**Releases**](https://github.com/toratechreal/SMS-Sync/releases) and install it (enable "install from unknown sources" if prompted).

> Or build from source: `./gradlew :app:assembleDebug`

### 3. Deploy the web client (optional)
1. Import this repo into [Vercel](https://vercel.com), root directory `web/`.
2. Add the **Vercel ↔ Supabase** integration (or set `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`).
3. Deploy.

### 4. Pair your devices
1. Open [`tools/pairing-qr-generator.html`](tools/pairing-qr-generator.html), paste your Supabase URL + key, and generate a QR.
2. In the app, scan the QR. Set one phone as **Host**, the rest as **Client**.
3. Grant the app SMS/contacts permissions when asked.

## Notes

- SMS only (v1).
- v1 has no auth; the Supabase anon key is the shared key. Don't expose it beyond people you trust.

## License

[GPL-3.0](LICENSE)

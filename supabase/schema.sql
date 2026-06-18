-- SMS Sync — Supabase schema
-- Run this once in your own Supabase project (SQL Editor) before pairing devices.
-- v1 is key-only (auth deferred): RLS is disabled so the anon key can read/write.
-- Tighten the RLS section when auth is added.

-- Store all timestamps in Eastern time for this project.
alter database postgres set timezone to 'America/New_York';

create table if not exists messages (
  id            uuid primary key default gen_random_uuid(),
  address       text not null,
  body          text not null,
  direction     text not null,            -- 'inbound' | 'outbound'
  sms_timestamp bigint not null,          -- original message time, ms epoch
  dedupe_key    text unique not null,     -- address|sms_timestamp|hash(body)
  read          boolean default false,
  created_at    timestamptz default now()
);

create table if not exists outgoing (
  id          uuid primary key default gen_random_uuid(),
  address     text not null,
  body        text not null,
  status      text default 'pending',     -- 'pending' | 'sent' | 'failed'
  error       text,
  created_at  timestamptz default now(),
  sent_at     timestamptz
);

create table if not exists device_status (
  device_id        text primary key,
  role             text,                  -- 'host' | 'client'
  phone_number     text,
  carrier          text,
  signal_strength  int,
  battery_level    int,
  battery_charging boolean,
  app_version      text,
  last_seen        timestamptz default now()
);

-- Helpful indexes for the client list/thread queries and the host queue drain.
create index if not exists messages_address_idx on messages (address);
create index if not exists messages_sms_timestamp_idx on messages (sms_timestamp);
create index if not exists outgoing_status_idx on outgoing (status);

-- Realtime: stream changes to all three tables.
alter publication supabase_realtime add table messages, outgoing, device_status;

-- v1: key-only access. RLS is ENABLED (Supabase requires this on public tables),
-- with wide-open policies so the anon key still works without auth.
--
-- KNOWN / ACCEPTED: the Supabase linter flags these policies as
-- "rls_policy_always_true" (WARN). This is expected and deliberate for v1 — auth
-- is deferred, so writes are intentionally open to anyone holding the anon key.
-- The fix is real authorization: add a user/owner column + Supabase Auth and
-- replace `using (true)` with user-scoped policies (e.g. using (auth.uid() = owner)).
alter table messages enable row level security;
alter table outgoing enable row level security;
alter table device_status enable row level security;

drop policy if exists "v1 anon full access" on messages;
drop policy if exists "v1 anon full access" on outgoing;
drop policy if exists "v1 anon full access" on device_status;

create policy "v1 anon full access" on messages
  for all to anon, authenticated using (true) with check (true);
create policy "v1 anon full access" on outgoing
  for all to anon, authenticated using (true) with check (true);
create policy "v1 anon full access" on device_status
  for all to anon, authenticated using (true) with check (true);

-- Contacts: names for numbers you've texted, uploaded from the host/client devices
-- (only numbers that appear in messages are uploaded — not the whole address book).
create table if not exists contacts (
  phone            text primary key,        -- normalized digits-only number
  name             text not null,
  source_device_id text,
  updated_at       timestamptz default now()
);

alter publication supabase_realtime add table contacts;

alter table contacts enable row level security;
drop policy if exists "v1 anon full access" on contacts;
create policy "v1 anon full access" on contacts
  for all to anon, authenticated using (true) with check (true);

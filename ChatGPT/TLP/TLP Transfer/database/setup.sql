-- TLP Transfer: public, no-login, no-password test edition.
-- WARNING: anybody on the Internet with the Supabase endpoint and public key
-- can read and submit replies. No access control, spam prevention or confidentiality.
-- Only approved public/non-sensitive content. To restrict access, change policies.
create table if not exists public.tlp_transfer_replies (
 id bigint generated always as identity primary key,
 message_id text not null check (message_id ~ '^TLP-[0-9]{8}-[0-9]{3}$'),
 body text not null check (char_length(btrim(body)) between 1 and 1000),
 created_at timestamptz not null default now()
);
create index if not exists tlp_transfer_replies_message_created_idx
 on public.tlp_transfer_replies (message_id, created_at, id);
alter table public.tlp_transfer_replies enable row level security;
-- Older installations may have an author_id column that rejects anonymous writes.
alter table public.tlp_transfer_replies drop column if exists author_id;
revoke all on table public.tlp_transfer_replies from public, anon, authenticated;
grant select, insert on table public.tlp_transfer_replies to anon, authenticated;
-- Supabase REST inserts may need USAGE on the identity sequence.
grant usage on sequence public.tlp_transfer_replies_id_seq to anon, authenticated;
drop policy if exists "TLP authenticated read replies" on public.tlp_transfer_replies;
drop policy if exists "TLP authenticated send replies" on public.tlp_transfer_replies;
drop policy if exists "TLP members can read replies" on public.tlp_transfer_replies;
drop policy if exists "TLP members can post own replies" on public.tlp_transfer_replies;
drop policy if exists "TLP public read replies" on public.tlp_transfer_replies;
drop policy if exists "TLP public send replies" on public.tlp_transfer_replies;
create policy "TLP public read replies" on public.tlp_transfer_replies
for select to anon, authenticated using (true);
create policy "TLP public send replies" on public.tlp_transfer_replies
for insert to anon, authenticated with check (true);
-- Updates and deletes are disabled to ordinary visitors.

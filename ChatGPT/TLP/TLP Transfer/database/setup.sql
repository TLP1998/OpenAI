-- TLP Transfer: simple one-password account, Supabase Free cloud replies.
-- Run this SQL in the Supabase project's SQL Editor.
-- Login is handled by Supabase Auth; GitHub Pages only stores public connection settings.
-- First create ONE account under Authentication > Users, and DISABLE public sign-ups.

create table if not exists public.tlp_transfer_replies (
  id bigint generated always as identity primary key,
  message_id text not null check (message_id ~ '^TLP-[0-9]{8}-[0-9]{3}$'),
  body text not null check (char_length(btrim(body)) between 1 and 1000),
  author_id uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);
create index if not exists tlp_transfer_replies_message_created_idx
  on public.tlp_transfer_replies (message_id, created_at, id);
alter table public.tlp_transfer_replies enable row level security;

revoke all on table public.tlp_transfer_replies from public, anon, authenticated;
grant select, insert on table public.tlp_transfer_replies to authenticated;

drop policy if exists "TLP authenticated read replies" on public.tlp_transfer_replies;
create policy "TLP authenticated read replies"
  on public.tlp_transfer_replies
  for select to authenticated
  using ((select auth.uid()) is not null);

drop policy if exists "TLP authenticated send replies" on public.tlp_transfer_replies;
create policy "TLP authenticated send replies"
  on public.tlp_transfer_replies
  for insert to authenticated
  with check (author_id = (select auth.uid()));

-- The table is append-only for web clients: no UPDATE/DELETE grants.
-- IMPORTANT: disable public sign-ups under Authentication settings, and
-- only create the authorized single account. Otherwise newly registered
-- accounts could access the replies.

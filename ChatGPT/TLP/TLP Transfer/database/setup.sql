-- TLP Transfer: Supabase free-plan online replies
-- Run this SQL once in Supabase Dashboard > SQL Editor.
-- Only approved accounts may read or send cloud replies.
-- Do NOT store secret/service_role keys in GitHub Pages.

create table if not exists public.tlp_transfer_members (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  active boolean not null default true,
  created_at timestamptz not null default now()
);
alter table public.tlp_transfer_members enable row level security;
revoke all on table public.tlp_transfer_members from public, anon, authenticated;

create or replace function public.tlp_transfer_is_member()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.tlp_transfer_members m
    where m.user_id = (select auth.uid()) and m.active = true
  );
$$;
revoke all on function public.tlp_transfer_is_member() from public, anon;
grant execute on function public.tlp_transfer_is_member() to authenticated;

create table if not exists public.tlp_transfer_replies (
  id bigint generated always as identity primary key,
  message_id text not null
    check (message_id ~ '^TLP-[0-9]{8}-[0-9]{3}$'),
  body text not null check (char_length(btrim(body)) between 1 and 1000),
  author_id uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);
create index if not exists tlp_transfer_replies_message_created_idx
  on public.tlp_transfer_replies (message_id, created_at, id);
alter table public.tlp_transfer_replies enable row level security;
revoke all on table public.tlp_transfer_replies from public, anon, authenticated;
grant select, insert on table public.tlp_transfer_replies to authenticated;

drop policy if exists "TLP members can read replies" on public.tlp_transfer_replies;
create policy "TLP members can read replies"
  on public.tlp_transfer_replies
  for select to authenticated
  using ((select public.tlp_transfer_is_member()));

drop policy if exists "TLP members can post own replies" on public.tlp_transfer_replies;
create policy "TLP members can post own replies"
  on public.tlp_transfer_replies
  for insert to authenticated
  with check (
    (select public.tlp_transfer_is_member())
    and author_id = (select auth.uid())
  );

-- No UPDATE / DELETE grants or policies: replies are append-only.
-- Enroll each authorized existing Auth user manually in the dashboard
-- using a separate SQL statement (replace the email address):
--
-- insert into public.tlp_transfer_members (user_id, display_name, active)
-- select id, 'Owner', true
-- from auth.users where email = 'YOUR_ACCOUNT_EMAIL@example.com'
-- on conflict (user_id)
-- do update set active = true;
--
-- Check enrollment (execute in SQL Editor only):
-- select m.user_id, u.email, m.active
-- from public.tlp_transfer_members m
-- join auth.users u on u.id = m.user_id;

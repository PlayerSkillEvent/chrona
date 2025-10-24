create extension if not exists "uuid-ossp";
create extension if not exists pgcrypto;

create table if not exists player (
  id uuid primary key,
  name text not null unique,
  locale text not null default 'en',
  first_join timestamptz not null default now(),
  last_seen  timestamptz,
  version int not null default 0,
  updated_at timestamptz not null default now()
);

create table if not exists wallet (
  player_id uuid primary key references player(id) on delete cascade,
  balance bigint not null default 0,           -- in Deben (Kupfer)
  version int not null default 0,              -- optimistic locking
  updated_at timestamptz not null default now()
);

create table if not exists econ_claim (
  id bigserial primary key,
  claim_id uuid not null unique,               -- external/business id
  player_id uuid not null references player(id) on delete cascade,
  source text not null,                        -- e.g. 'QUEST_ARC-PH1-03'
  amount bigint not null check (amount > 0),
  created_at timestamptz not null default now()
);

create table if not exists econ_transfer (
  transfer_id uuid primary key default gen_random_uuid(),
  from_player uuid references player(id) on delete set null,
  to_player   uuid references player(id) on delete set null,
  amount      bigint not null check (amount > 0),
  reason      text not null,                   -- 'PAY', 'ADMIN_MINT', 'FEE', ...
  corr_id     uuid,                            -- optional correlation id for logs
  created_at  timestamptz not null default now(),
  -- At least one side must be non-null
  check ( (from_player is not null) or (to_player is not null) )
);

create index if not exists idx_wallet_updated_at on wallet(updated_at desc);
create index if not exists idx_claim_player on econ_claim(player_id);
create index if not exists idx_transfer_from on econ_transfer(from_player);
create index if not exists idx_transfer_to   on econ_transfer(to_player);

create table if not exists player(
  id uuid primary key,
  name text not null unique,
  locale text not null default 'en',
  first_join timestamptz not null default now(),
  last_seen timestamptz,
  version int not null default 0,
  updated_at timestamptz not null default now()
);

create table if not exists wallet(
  player_id uuid primary key references player(id) on delete cascade,
  balance bigint not null default 0,
  version int not null default 0,
  updated_at timestamptz not null default now()
);

create table if not exists econ_claim(
  id bigserial primary key,
  claim_id uuid not null unique,
  player_id uuid not null references player(id) on delete cascade,
  source text not null,
  amount bigint not null,
  created_at timestamptz not null default now()
);

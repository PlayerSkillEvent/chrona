create table if not exists econ_transfer (
  transfer_id uuid primary key,
  from_player uuid not null references player(id) on delete cascade,
  to_player   uuid not null references player(id) on delete cascade,
  amount      bigint not null check (amount > 0),
  created_at  timestamptz not null default now()
);

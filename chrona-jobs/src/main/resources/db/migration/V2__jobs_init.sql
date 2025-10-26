create table if not exists job (
  id text primary key,
  display_name text not null
);

create table if not exists job_player (
  player_id uuid not null references player(id) on delete cascade,
  job_id text not null references job(id) on delete cascade,
  level int not null default 1,
  xp bigint not null default 0,
  last_action_at timestamptz,
  season text,
  primary key (player_id, job_id, season)
);

create table if not exists job_run (
  id uuid primary key,
  player_id uuid not null references player(id) on delete cascade,
  job_id text not null references job(id) on delete cascade,
  season text,
  payload jsonb,
  created_at timestamptz not null default now()
);

create table if not exists job_reward_claim (
  claim_id uuid primary key,
  job_run_id uuid not null references job_run(id) on delete cascade,
  player_id uuid not null references player(id) on delete cascade,
  amount bigint not null,
  created_at timestamptz not null default now()
);

create index if not exists idx_job_player_job on job_player(job_id);
create index if not exists idx_job_run_player on job_run(player_id, created_at desc);

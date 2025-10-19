alter table if exists econ_claim
  add column if not exists source_domain text,
  add column if not exists source_key    text;

do $$
begin
  if exists (select 1 from information_schema.columns
             where table_name='econ_claim' and column_name='source') then
    update econ_claim
       set source_domain = coalesce(source_domain,
           case
             when source ilike '%grant%' then 'SYSTEM'
             when source ilike '%pay%' or source ilike '%trade%' then 'ECONOMY'
             else 'ECONOMY'
           end),
           source_key = coalesce(source_key,
           case
             when source ilike '%grant%' then 'admin_grant'
             when source ilike '%pay%' or source ilike '%trade%' then 'player_trade'
             else regexp_replace(source, '[^a-z0-9_]+', '_', 'gi')
           end);
  end if;
end $$;

update econ_claim set source_domain = 'ECONOMY' where source_domain is null;
update econ_claim set source_key    = 'other'   where source_key    is null;

alter table econ_claim
  alter column source_domain set not null,
  alter column source_key    set not null;

create index if not exists idx_claim_source on econ_claim(source_domain, source_key);

alter table econ_claim drop column if exists source;

alter table if exists econ_transfer
  add column if not exists source_domain text,
  add column if not exists source_key    text;

update econ_transfer set source_domain = 'ECONOMY' where source_domain is null;
update econ_transfer set source_key    = 'player_trade' where source_key is null;

alter table econ_transfer
  alter column source_domain set not null,
  alter column source_key    set not null;

create index if not exists idx_transfer_source on econ_transfer(source_domain, source_key);

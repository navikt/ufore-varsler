alter table varsel
    add column varsel_id uuid not null default gen_random_uuid();

alter table varsel
    drop column planlagt_utsending;
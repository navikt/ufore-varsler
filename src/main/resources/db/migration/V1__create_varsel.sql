create table varsel (
    id uuid primary key,
    mottaker_fnr varchar not null,
    status varchar not null,
    type varchar not null,
    opprettet timestamp not null,
    planlagt_utsending timestamp,
    aapnet timestamp,
    sendt timestamp
);

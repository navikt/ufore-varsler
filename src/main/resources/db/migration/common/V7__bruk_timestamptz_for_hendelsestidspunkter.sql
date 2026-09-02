alter table varsel
    alter column opprettet set data type timestamp with time zone using opprettet at time zone 'Europe/Oslo';

alter table varsel
    alter column bestilt set data type timestamp with time zone using bestilt at time zone 'Europe/Oslo';

alter table varsel
    alter column sendt set data type timestamp with time zone using sendt at time zone 'Europe/Oslo';

alter table varsel
    alter column aapnet set data type timestamp with time zone using aapnet at time zone 'Europe/Oslo';

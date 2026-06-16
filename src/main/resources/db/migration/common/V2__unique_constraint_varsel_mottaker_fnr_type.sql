alter table varsel
    add constraint varsel_mottaker_fnr_type unique (mottaker_fnr, type);

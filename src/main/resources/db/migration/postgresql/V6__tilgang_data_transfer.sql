DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_user where usename = 'ufore-varsler-data-transfer')
        THEN
            GRANT SELECT ON varsel TO "ufore-varsler-data-transfer";
        END IF;
    END
$$;
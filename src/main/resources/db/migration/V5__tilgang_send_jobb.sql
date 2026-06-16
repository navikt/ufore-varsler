DO
$$
BEGIN
    IF EXISTS
        (SELECT 1 FROM pg_user where usename = 'ufore-varsler-send-jobb')
    THEN
        GRANT SELECT, UPDATE ON varsel TO "ufore-varsler-send-jobb";
    END IF;
END
$$;
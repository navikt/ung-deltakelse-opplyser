ALTER TABLE IF EXISTS ungdomsprogram_deltakelse
    DROP COLUMN IF EXISTS har_sokt,
    ADD COLUMN søkt_tidspunkt TIMESTAMP NULL;

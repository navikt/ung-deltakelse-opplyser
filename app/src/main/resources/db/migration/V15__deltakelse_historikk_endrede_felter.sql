ALTER TABLE IF EXISTS ungdomsprogram_deltakelse_historikk
    ADD COLUMN periode_MOD BOOLEAN DEFAULT FALSE,
    ADD COLUMN søkt_tidspunkt_MOD BOOLEAN DEFAULT FALSE

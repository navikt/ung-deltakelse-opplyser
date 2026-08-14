ALTER TABLE IF EXISTS ungdomsprogram_deltakelse
    ADD COLUMN avslutningsarsak VARCHAR(50) NULL;

ALTER TABLE IF EXISTS ungdomsprogram_deltakelse_historikk
    ADD COLUMN avslutningsarsak VARCHAR(50) NULL;

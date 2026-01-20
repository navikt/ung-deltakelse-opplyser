ALTER TABLE IF EXISTS ungdomsprogram_deltakelse
    ADD COLUMN har_opphoersvedtak BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE IF EXISTS ungdomsprogram_deltakelse_historikk
    ADD COLUMN har_opphoersvedtak BOOLEAN DEFAULT FALSE NOT NULL;
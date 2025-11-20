ALTER TABLE IF EXISTS ungdomsprogram_deltakelse
    ADD COLUMN er_slettet BOOLEAN DEFAULT FALSE NOT NULL;

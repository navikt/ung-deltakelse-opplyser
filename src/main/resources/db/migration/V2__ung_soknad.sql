CREATE TABLE IF NOT EXISTS ung_søknad
(
    journalpost_id VARCHAR(50) PRIMARY KEY NOT NULL,
    søker_ident    VARCHAR(20)             NOT NULL,
    søknad         jsonb                   NOT NUll,
    opprettet_dato TIMESTAMP               NOT NULL,
    oppdatert_dato TIMESTAMP               NOT NULL,
);


-- Indeks på 'søker_ident' som optimaliserer spørringer på kolonnen 'søker_ident'.
CREATE INDEX idx_ung_soknad_soker_ident ON ung_søknad(søker_ident);

-- GIN-indeks på 'søknad' for JSONB-operasjoner.
-- Den forbedrer ytelsen på spørringer som benytter søk etter bestemte nøkkel/verdi-par i JSON-strukturen.
CREATE INDEX idx_ung_soknad_soknad ON ung_søknad USING gin (søknad);

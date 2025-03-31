CREATE TABLE IF NOT EXISTS ung_rapportert_inntekt
(
    journalpost_id VARCHAR(50) PRIMARY KEY NOT NULL,
    søker_ident    VARCHAR(20)             NOT NULL,
    inntekt        jsonb                   NOT NUll,
    opprettet_dato TIMESTAMP               NOT NULL,
    oppdatert_dato TIMESTAMP               NOT NULL
);


-- Indeks på 'søker_ident' som optimaliserer spørringer på kolonnen 'søker_ident'.
CREATE INDEX IF NOT EXISTS idx_ung_rapportert_inntekt_soker_ident ON ung_rapportert_inntekt (søker_ident);

-- GIN-indeks på 'inntekt' for JSONB-operasjoner.
-- Den forbedrer ytelsen på spørringer som benytter søk etter bestemte nøkkel/verdi-par i JSON-strukturen.
CREATE INDEX IF NOT EXISTS idx_ung_rapportert_inntekt_inntekt ON ung_rapportert_inntekt USING gin (inntekt);

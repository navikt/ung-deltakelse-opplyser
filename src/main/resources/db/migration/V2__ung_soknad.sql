CREATE TABLE ung_søknad
(
    journalpost_id VARCHAR(50) PRIMARY KEY NOT NULL,
    søker_ident    VARCHAR(20)             NOT NULL,
    søknad         jsonb                   NOT NUll,
    opprettet_dato TIMESTAMP               NOT NULL,
    oppdatert_dato TIMESTAMP               NOT NULL
);

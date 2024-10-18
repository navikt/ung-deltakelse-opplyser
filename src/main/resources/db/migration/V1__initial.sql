CREATE TABLE IF NOT EXISTS ungdomsprogram_deltakelse
(
    id                  UUID PRIMARY KEY NOT NULL,
    deltaker_ident      VARCHAR(20)      NOT NULL,
    periode             DATERANGE        NOT NULL,
    opprettet_tidspunkt TIMESTAMP        NOT NULL,
    endret_tidspunkt    TIMESTAMP,
    CONSTRAINT ingen_overlappende_periode EXCLUDE USING GIST (deltaker_ident WITH =, periode WITH &&)
);


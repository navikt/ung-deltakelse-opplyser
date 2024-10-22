CREATE TABLE IF NOT EXISTS ungdomsprogram_deltakelse
(
    id                  UUID PRIMARY KEY NOT NULL,
    deltaker_ident      VARCHAR(20)      NOT NULL,
    periode             DATERANGE        NOT NULL,
    opprettet_tidspunkt TIMESTAMP        NOT NULL,
    endret_tidspunkt    TIMESTAMP,

    -- Sjekker at det ikke er overlappende perioder for samme deltaker
    CONSTRAINT ingen_overlappende_periode EXCLUDE USING GIST (deltaker_ident WITH =, periode WITH &&)
);


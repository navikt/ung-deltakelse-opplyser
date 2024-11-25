DROP TABLE IF EXISTS ungdomsprogram_deltakelse;
DROP TABLE IF EXISTS deltaker;

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE IF NOT EXISTS deltaker
(
    id             UUID PRIMARY KEY NOT NULL,
    deltaker_ident VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS ungdomsprogram_deltakelse
(
    id                  UUID PRIMARY KEY NOT NULL,
    deltaker_id         UUID             NOT NULL, -- Referanse til deltaker
    periode             DATERANGE        NOT NULL,
    har_sokt            BOOLEAN          NOT NULL DEFAULT FALSE,
    opprettet_tidspunkt TIMESTAMP        NOT NULL,
    endret_tidspunkt    TIMESTAMP,

    -- Sjekker at det ikke er overlappende perioder for samme deltaker
    CONSTRAINT ingen_overlappende_periode EXCLUDE USING GIST (deltaker_id WITH =, periode WITH &&),

    -- Referanser
    -- Sjekker at deltaker_id refererer til en eksisterende deltaker
    CONSTRAINT fk_deltaker_id FOREIGN KEY (deltaker_id) REFERENCES deltaker(id)
);

ALTER TABLE IF EXISTS ungdomsprogram_deltakelse
    ADD COLUMN opprettet_av TEXT NULL,
    ADD COLUMN endret_av    TEXT NULL;

CREATE SEQUENCE IF NOT EXISTS revinfo_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS revinfo
(
    rev      INTEGER PRIMARY KEY NOT NULL, -- Version number.
    revtstmp BIGINT                        -- Epoch timestamp of the version number.
);

CREATE TABLE IF NOT EXISTS ungdomsprogram_deltakelse_historikk
(
    -- Påkrevde felt for historikk
    id                  UUID,
    rev                 INTEGER REFERENCES revinfo (rev), -- Versjonnummeret for entiteten.
    revend              INTEGER REFERENCES revinfo (rev), -- Versjonnummeret for neste versjon etter at entiten blir oppdatert.
    revtype             SMALLINT,                         -- Type endring, 1=insert, 2=update, 3=delete.
    revend_tstmp        TIMESTAMP,                        -- Tidsspunkt for neste versjon etter at entiteten blir oppdatert.
    -- Påkrevde felt historikk

    periode             DATERANGE NOT NULL,
    søkt_tidspunkt      TIMESTAMP NULL,
    opprettet_tidspunkt TIMESTAMP,
    endret_tidspunkt    TIMESTAMP,
    opprettet_av        TEXT,
    endret_av           TEXT
);

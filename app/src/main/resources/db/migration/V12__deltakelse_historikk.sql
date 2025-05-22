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
    id                  UUID,
    rev                 INTEGER REFERENCES revinfo (rev), -- The version number of the entity.
    revend              INTEGER REFERENCES revinfo (rev), -- The version of the next version number after entity gets updated.
    revtype             SMALLINT,                         -- The type of the revision.
    revend_tstmp        TIMESTAMP,                        -- The timestamp of the next version number after entity gets updated.
    periode             DATERANGE NOT NULL,
    søkt_tidspunkt      TIMESTAMP NULL,
    opprettet_tidspunkt TIMESTAMP,
    endret_tidspunkt    TIMESTAMP,
    opprettet_av        TEXT,
    endret_av           TEXT
);

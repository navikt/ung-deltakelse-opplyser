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
    periode             DATERANGE        NOT NULL,
    har_sokt            BOOLEAN,
    opprettet_tidspunkt TIMESTAMP NOT NULL,
    endret_tidspunkt    TIMESTAMP
);

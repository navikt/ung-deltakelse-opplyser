CREATE TABLE IF NOT EXISTS deltakelse_veileder_enhet
(
    deltakelse_id       UUID PRIMARY KEY NOT NULL,
    nav_ident           TEXT             NOT NULL,
    enhet_id            TEXT             NOT NULL,
    enhet_navn          TEXT             NOT NULL,
    opprettet_tidspunkt TIMESTAMP        NOT NULL DEFAULT now(),

    CONSTRAINT fk_deltakelse_veileder_enhet_deltakelse FOREIGN KEY (deltakelse_id)
        REFERENCES ungdomsprogram_deltakelse (id)
);

CREATE INDEX idx_deltakelse_veileder_enhet_nav_ident ON deltakelse_veileder_enhet (nav_ident);


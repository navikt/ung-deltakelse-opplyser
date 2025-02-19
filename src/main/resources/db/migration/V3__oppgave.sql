CREATE TABLE IF NOT EXISTS oppgave
(
    id            UUID PRIMARY KEY NOT NULL,
    deltakelse_id UUID             NOT NULL,
    oppgavetype   VARCHAR(50)      NOT NULL,
    status        VARCHAR(50)      NOT NULL,
    opprettet_dato TIMESTAMP NOT NULL,
    løst_dato TIMESTAMP,

    CONSTRAINT fk_oppgaver_deltakelseId FOREIGN KEY (deltakelse_id) REFERENCES ungdomsprogram_deltakelse (id)
);

-- Partial unique index: For hver deltakelse kan det maksimalt finnes én ULØST oppgave per oppgavetype
CREATE UNIQUE INDEX unique_ulost_oppgavetype ON oppgave (deltakelse_id, oppgavetype)
    WHERE status = 'ULØST';

CREATE TABLE ungdomsprogram
(
    id             UUID PRIMARY KEY NOT NULL,
    deltaker_ident VARCHAR(20)      NOT NULL,
    fra_og_med     DATE             NOT NULL,
    til_og_med     DATE,
    opprettet_dato TIMESTAMP        NOT NULL,
    oppdatert_dato TIMESTAMP        NOT NULL
);

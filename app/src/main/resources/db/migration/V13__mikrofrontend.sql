CREATE TABLE mikrofrontend
(
    id               UUID PRIMARY KEY NOT NULL,
    deltaker_id      UUID             NOT NULL, -- Referanse til deltaker
    mikrofrontend_id VARCHAR(100)     NOT NULL,
    status           VARCHAR(10)      NOT NULL,
    opprettet        TIMESTAMP        NOT NULL,
    endret           TIMESTAMP,

    CONSTRAINT uk_mikrofrontend_deltaker_mf UNIQUE (deltaker_id, mikrofrontend_id)
)

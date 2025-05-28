DROP TABLE IF EXISTS mikrofrontend;

CREATE TABLE IF NOT EXISTS min_side_microfrontend_status
(
    id          UUID PRIMARY KEY NOT NULL,
    deltaker_id UUID             NOT NULL, -- Referanse til deltaker
    status      VARCHAR(10)      NOT NULL,
    opprettet   TIMESTAMP        NOT NULL,
    endret      TIMESTAMP,

    -- Sjekker at deltaker_id refererer til en eksisterende deltaker
    CONSTRAINT fk_deltaker_id FOREIGN KEY (deltaker_id) REFERENCES deltaker (id)
)

-- Tømmer oppgave-tabellen
TRUNCATE TABLE oppgave;

-- Fjern koblingen mot deltakelse
ALTER TABLE oppgave
    DROP CONSTRAINT IF EXISTS fk_oppgaver_deltakelseId,
    DROP COLUMN IF EXISTS deltakelse_id;

-- Legg til kolonnen for kobling til deltaker
ALTER TABLE oppgave
    ADD COLUMN IF NOT EXISTS deltaker_id UUID;

-- Legg til kobling mot deltaker
ALTER TABLE oppgave
    ADD CONSTRAINT fk_oppgave_deltaker FOREIGN KEY (deltaker_id)
        REFERENCES deltaker (id);

-- Legg til constraint som hindrer at en deltaker har to oppgaver med samme type og uløst status.
-- Unntak for oppgavetype 'BEKREFT_AVVIK_REGISTERINNTEKT' da denne kan ha flere uløste oppgaver.
CREATE UNIQUE INDEX uniq_oppgave_deltaker_oppgavetype_ulost
    ON oppgave (deltaker_id, oppgavetype)
    WHERE status = 'ULØST' AND oppgavetype NOT IN ('BEKREFT_AVVIK_REGISTERINNTEKT');

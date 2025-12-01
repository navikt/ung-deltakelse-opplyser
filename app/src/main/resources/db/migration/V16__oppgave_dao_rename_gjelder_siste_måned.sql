UPDATE oppgave
SET oppgavetype_data = jsonb_set(
        oppgavetype_data - 'gjelderSisteMåned', -- Fjerner gammel felt
        '{gjelderDelerAvMåned}', -- Setter nytt felt
        to_jsonb((oppgave_bekreftelse ->> 'gjelderSisteMåned')::boolean), -- kopierer verdien
        false -- Oppretter felt hvis det ikke finnes og
                          )
WHERE oppgavetype_data IS NOT NULL and oppgavetype in ('RAPPORTER_INNTEKT', 'BEKREFT_AVVIK_REGISTERINNTEKT');
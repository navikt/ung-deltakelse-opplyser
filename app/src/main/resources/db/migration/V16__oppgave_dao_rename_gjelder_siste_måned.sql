UPDATE oppgave
SET oppgavetype_data =jsonb_set(
        oppgavetype_data - 'gjelderSisteMåned', -- Fjerner gammel felt
        '{gjelderDelerAvMåned}', -- Setter nytt felt
        to_jsonb(coalesce(oppgavetype_data ->> 'gjelderSisteMåned', 'false')), -- kopierer verdien
        true -- Oppretter felt hvis det ikke finnes og
                      ) from oppgave
WHERE oppgavetype_data IS NOT NULL and oppgavetype in ('RAPPORTER_INNTEKT', 'BEKREFT_AVVIK_REGISTERINNTEKT');
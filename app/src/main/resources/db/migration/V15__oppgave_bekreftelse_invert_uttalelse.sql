UPDATE oppgave
SET oppgave_bekreftelse = jsonb_set(
        oppgave_bekreftelse - 'harGodtattEndringen', -- Fjerner gammel felt
        '{harUttalelse}', -- Setter nytt felt
        to_jsonb(NOT (oppgave_bekreftelse ->> 'harGodtattEndringen')::boolean), -- Inverterer verdien
        true -- Oppretter felt hvis det ikke finnes. Da harUttalelse ikke eksistert før, må dette settes til true.
                          )
WHERE oppgave_bekreftelse IS NOT NULL
  AND oppgave_bekreftelse ? 'harGodtattEndringen'; -- Sjekker at feltet finnes

ALTER TABLE IF EXISTS ungdomsprogram_deltakelse_historikk
    ADD COLUMN har_opphoersvedtak BOOLEAN DEFAULT FALSE NOT NULL;

-- Opphørsvedtak som er markert før feltet ble auditert, har gitt en revisjon uten kjent endring (UKJENT).
-- Setter feltet på siste revisjon slik at den vises som opphørsvedtak i historikken.
UPDATE ungdomsprogram_deltakelse_historikk h
SET har_opphoersvedtak = TRUE
FROM ungdomsprogram_deltakelse d
WHERE h.id = d.id
  AND d.har_opphoersvedtak = TRUE
  AND h.revend IS NULL;

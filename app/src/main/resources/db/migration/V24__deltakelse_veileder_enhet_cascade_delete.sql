-- Legger til ON DELETE CASCADE på FK fra deltakelse_veileder_enhet til ungdomsprogram_deltakelse.
-- Dette sikrer at veileder-enhet rader automatisk slettes når deltakelsen slettes.
ALTER TABLE deltakelse_veileder_enhet
    DROP CONSTRAINT fk_deltakelse_veileder_enhet_deltakelse,
    ADD CONSTRAINT fk_deltakelse_veileder_enhet_deltakelse
        FOREIGN KEY (deltakelse_id) REFERENCES ungdomsprogram_deltakelse (id) ON DELETE CASCADE;

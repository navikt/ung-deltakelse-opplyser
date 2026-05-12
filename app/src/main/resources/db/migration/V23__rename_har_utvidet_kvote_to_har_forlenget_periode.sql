ALTER TABLE ungdomsprogram_deltakelse
    RENAME COLUMN har_utvidet_kvote TO har_forlenget_periode;

ALTER TABLE ungdomsprogram_deltakelse_historikk
    RENAME COLUMN har_utvidet_kvote TO har_forlenget_periode;

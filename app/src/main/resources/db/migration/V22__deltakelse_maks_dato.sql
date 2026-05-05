-- Legger til maks_dato på deltakelse-tabellen.
-- Brukes til å schedulere automatisk opphør når maksdato er nådd.
alter table ungdomsprogram_deltakelse add column maks_dato date;

comment on column ungdomsprogram_deltakelse.maks_dato is 'Beregnet maksdato for deltakelsen (260/300 virkedager fra startdato). Settes av veileder ved innmelding.';

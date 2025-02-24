package no.nav.ung.deltakelseopplyser.minesider.varsler

import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class MineSiderVarselService(
    @Value("\${topic.producer.min-side-varsel.navn}") private val minSideVarselTopic: String,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(MineSiderVarselService::class.java)
    }

    /**
     * Oppretter et varsel for en oppgave
     *
     * @param oppgaveId Id til varselet. Produsenten bruker samme Id til å inaktivere varsel.
     * @param deltakerIdent Fodselsnummer (evt. d-nummer eller tilsvarende) til mottaker av varsel.
     * @param oppgavetekster Teksten som faktisk vises i varselet med språkkode.
     * @param oppgavelenke Lenke som blir aktivert når en person trykker på varselet i varselbjella eller på min side.
     */
    fun opprettOppgve(oppgaveId: String, deltakerIdent: String, oppgavetekster: List<Tekst>, oppgavelenke: String) {
        logger.info("Oppretter min-side oppgave med id $oppgaveId")

        val opprett: String = VarselActionBuilder.opprett {
            /**
             * Type på varsel (beskjed, oppgave, innboks)
             */
            type = Varseltype.Oppgave

            /**
             * Id til varselet. Produsenten bruker samme Id til å inaktivere varsel.
             */
            varselId = oppgaveId

            /**
             * Påkrevd level-of-assurance for å kunne se innhold i varsel.
             * Én av high, substantial.
             */
            sensitivitet = Sensitivitet.High

            /**
             * Fodselsnummer (evt. d-nummer eller tilsvarende) til mottaker av varsel
             */
            ident = deltakerIdent

            /**
             * Teksten som faktisk vises i varselet med språkkode.
             * Dersom flere tekster på ulike språk er gitt må én tekst være satt som default.
             * Språkkode må følge ISO-639 (typen no, nb, en...)
             */
            tekster += oppgavetekster

            /**
             * Lenke som blir aktivert når en person trykker på varselet i varselbjella eller på min side
             * Ikke for beskjed.
             * Må være enk omplett URL, inkludert https protokoll.
             */
            link = oppgavelenke

            /**
             * Tidspunkt for når varslet skal inaktiverer automatisk av systemet.
             * Tidspunkt med tidssone. UTC eller Z er anbefalt.
             * Støttes ikke for Innboks-varsler.
             */

            /**
             * Om det skal sendes sms og/eller epost til mottaker.
             * Kan kun velge preferert kanal SMS eller EPOST.
             * Dersom ekstern varslingstekst ikke er satt blir det sendt en standardtekst.
             */
            eksternVarsling {
                preferertKanal = EksternKanal.SMS
            }
        }

        kafkaTemplate.send(minSideVarselTopic, oppgaveId, opprett)
            .whenComplete { sendResult, exception ->
                if (exception != null) {
                    val feilmelding =
                        "Feilet med å publisere min-side oppgave til topic $minSideVarselTopic med key $oppgaveId"
                    logger.error(feilmelding, exception)
                    throw RuntimeException(feilmelding, exception)
                } else {
                    logger.info("Publiserte min-side oppgave: {}", sendResult.recordMetadata.toString())

                    // TODO: Lagre oppgave.
                }
            }
    }

    /**
     * Deaktiverer et varsel.
     * @param oppgaveId Id til varselet som skal deaktiveres.
     */
    fun deaktiverOppgave(oppgaveId: String) {
        /**
         * Deaktiverer et varsel
         *
         * @param oppgaveId Id til varselet som skal deaktiveres.
         */
        val inaktiver = VarselActionBuilder.inaktiver {
            varselId = oppgaveId
        }

        kafkaTemplate.send(minSideVarselTopic, oppgaveId, inaktiver)
            .whenComplete { sendResult, exception ->
                if (exception != null) {
                    val feilmelding =
                        "Feilet med å publisere inaktivering av min-side oppgave til topic $minSideVarselTopic med key $oppgaveId"
                    logger.error(feilmelding, exception)
                    throw RuntimeException(feilmelding, exception)
                } else {
                    logger.info("Publiserte inaktivering av min-side oppgave: {}", sendResult.recordMetadata.toString())

                    // TODO: Deaktiver oppgave i database.
                }
            }
    }
}

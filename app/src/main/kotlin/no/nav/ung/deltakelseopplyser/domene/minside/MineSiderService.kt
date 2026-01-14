package no.nav.ung.deltakelseopplyser.domene.minside

import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import no.nav.ung.deltakelseopplyser.domene.minside.mikrofrontend.MicrofrontendId
import no.nav.ung.deltakelseopplyser.integration.pdl.api.PdlService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@Service
class MineSiderService(
    private val pdlService: PdlService,
    @Value("\${topic.producer.min-side-varsel.navn}") private val minSideVarselTopic: String,
    @Value("\${topic.producer.min-side-mikrofrontend.navn}") private val minSideMikrofrontendTopic: String,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${NAIS_CLUSTER_NAME}") private val cluster: String,
    @Value("\${NAIS_NAMESPACE}") private val namespace: String,
    @Value("\${NAIS_APP_NAME}") private val appName: String,

    ) {
    private companion object {
        private val logger = LoggerFactory.getLogger(MineSiderService::class.java)
    }

    /**
     * Oppretter et varsel for en oppgave
     *
     * @param varselId Id til varselet. Produsenten bruker samme Id til å inaktivere varsel.
     * @param deltakerIdent Fodselsnummer (evt. d-nummer eller tilsvarende) til mottaker av varsel.
     * @param tekster Teksten som faktisk vises i varselet med språkkode.
     * @param varselLink Lenke som blir aktivert når en person trykker på varselet i varselbjella eller på min side.
     * @param varseltype Type på varsel (beskjed, oppgave, innboks).
     * @param aktivFremTil Tidspunkt for når varslet skal inaktiverer automatisk av systemet.
     */
    fun opprettVarsel(
        varselId: String,
        deltakerIdent: String,
        tekster: List<Tekst>,
        varselLink: String,
        varseltype: Varseltype,
        aktivFremTil: ZonedDateTime? = null
    ) {
        logger.info("Oppretter min-side oppgave med id $varselId")

        val aktivFolkeregisterIdent = pdlService.hentFolkeregisteridenter(deltakerIdent).first { !it.historisk }.ident

        val opprett: String = VarselActionBuilder.opprett {
            /**
             * Type på varsel (beskjed, oppgave, innboks)
             */
            type = varseltype

            /**
             * Id til varselet. Produsenten bruker samme Id til å inaktivere varsel.
             */
            this.varselId = varselId

            /**
             * Påkrevd level-of-assurance for å kunne se innhold i varsel.
             * Én av high, substantial.
             */
            sensitivitet = Sensitivitet.High

            /**
             * Fodselsnummer (evt. d-nummer eller tilsvarende) til mottaker av varsel
             */
            ident = aktivFolkeregisterIdent

            /**
             * Teksten som faktisk vises i varselet med språkkode.
             * Dersom flere tekster på ulike språk er gitt må én tekst være satt som default.
             * Språkkode må følge ISO-639 (typen no, nb, en...)
             */
            this.tekster += tekster

            /**
             * Lenke som blir aktivert når en person trykker på varselet i varselbjella eller på min side
             * Ikke for beskjed.
             * Må være enk omplett URL, inkludert https protokoll.
             */
            link = varselLink

            /**
             * Tidspunkt for når varslet skal inaktiverer automatisk av systemet.
             * Tidspunkt med tidssone. UTC eller Z er anbefalt.
             * Støttes ikke for Innboks-varsler.
             */
            this.aktivFremTil = aktivFremTil

            /**
             * Om det skal sendes sms og/eller epost til mottaker.
             * Kan kun velge preferert kanal SMS eller EPOST.
             * Dersom ekstern varslingstekst ikke er satt blir det sendt en standardtekst.
             */
            eksternVarsling {
                preferertKanal = EksternKanal.SMS
            }

            produsent = produsent()
        }

        kafkaTemplate.send(minSideVarselTopic, varselId, opprett)
            .whenComplete { sendResult, exception ->
                if (exception != null) {
                    val feilmelding =
                        "Feilet med å publisere min-side oppgave til topic $minSideVarselTopic med key $varselId"
                    logger.error(feilmelding, exception)
                    throw RuntimeException(feilmelding, exception)
                } else {
                    logger.info("Publiserte min-side oppgave: {}", sendResult.recordMetadata.toString())
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
            produsent = produsent()
        }

        val result: SendResult<String, String> = kafkaTemplate
            .send(minSideVarselTopic, oppgaveId, inaktiver)
            .get(60, TimeUnit.SECONDS)

        logger.info("Publiserte inaktivering av min-side oppgave: {}", result.recordMetadata.toString())
    }

    fun aktiverMikrofrontend(
        deltakerIdent: String,
        microfrontendId: MicrofrontendId ,
        sensitivitet: no.nav.tms.microfrontend.Sensitivitet
    ) {
        val aktivFolkeregisterIdent = pdlService.hentFolkeregisteridenter(deltakerIdent).first { !it.historisk }.ident

        val enable = MicrofrontendMessageBuilder.enable {
            ident = aktivFolkeregisterIdent
            initiatedBy = namespace
            this.microfrontendId = microfrontendId.id
            this.sensitivitet = sensitivitet
        }.text()

        val result = kafkaTemplate
            .send(minSideMikrofrontendTopic, microfrontendId.id, enable)
            .get(60, TimeUnit.SECONDS)
        logger.info("Publiserte aktivering av mikrofrontend: {}", result.recordMetadata.toString())
    }

    fun deaktiverMikrofrontend(
        deltakerIdent: String,
        microfrontendId: MicrofrontendId,
    ) {
        val disable = MicrofrontendMessageBuilder.disable {
            ident = deltakerIdent
            initiatedBy = namespace
            this.microfrontendId = microfrontendId.id
        }.text()

        kafkaTemplate.send(minSideMikrofrontendTopic, microfrontendId.id, disable)
            .whenComplete { sendResult, exception ->
                if (exception != null) {
                    val feilmelding =
                        "Feilet med å publisere deaktivering av mikrofrontend til topic $minSideMikrofrontendTopic"
                    logger.error(feilmelding, exception)
                    throw RuntimeException(feilmelding, exception)
                } else {
                    logger.info("Publiserte deaktivering av mikrofrontend: {}", sendResult.recordMetadata.toString())
                }
            }
    }

    private fun produsent() = Produsent(
        cluster = cluster,
        namespace = namespace,
        appnavn = appName,
    )
}

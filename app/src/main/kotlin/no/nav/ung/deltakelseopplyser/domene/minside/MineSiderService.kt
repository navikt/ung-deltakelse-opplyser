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
    @Value("\${topic.producer.min-side-mikrofrontend.navn}") private val minSideMikrofrontendTopic: String,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${NAIS_CLUSTER_NAME}") private val cluster: String,
    @Value("\${NAIS_NAMESPACE}") private val namespace: String,
    @Value("\${NAIS_APP_NAME}") private val appName: String,

    ) {
    private companion object {
        private val logger = LoggerFactory.getLogger(MineSiderService::class.java)
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

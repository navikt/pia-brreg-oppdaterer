package brreg

import brreg.Miljø.KAFKA_TOPIC
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OppdateringService(private val brregApi: BrregApi, private val kafkaProdusent: KafkaProdusent) {

    suspend fun oppdater() {
        val days = Miljø.ANTALL_DAGER_SIDEN_OPPDATERING.toLong()
        val tidspunkt = ZonedDateTime.now(ZoneOffset.UTC).minusDays(days)
        var skalSøkeMer = true
        var sideAntall = 0
        while (skalSøkeMer) {
            val (_embedded, _, page) = brregApi.hentOppdaterteUnderenheter(tidspunkt, sideAntall)
            val enheterGruppertPåType = _embedded.oppdaterteUnderenheter.groupBy { it.endringstype }
            enheterGruppertPåType.entries.forEach { (key, value) ->
                sendTilKafka(endringstype = key, enheter = value)
            }
            skalSøkeMer = page.number != (page.totalPages - 1)
            sideAntall += 1
        }
    }

    private suspend fun sendTilKafka(endringstype: Endringstype, enheter: List<OppdateringDTO>) {
        val underenheterFraBrreg = hentEndredeUnderenheter(endringstype, enheter)
        enheter.forEach { enhet ->
            val melding = OppdateringVirksomhet(
                oppdateringsid = enhet.oppdateringsid,
                orgnummer = enhet.organisasjonsnummer,
                endringstype = endringstype,
                endringstidspunkt = enhet.dato,
                metadata = underenheterFraBrreg.find { it.organisasjonsnummer == enhet.organisasjonsnummer }
            )
            kafkaProdusent.sendMelding(KAFKA_TOPIC, enhet.organisasjonsnummer, Json.encodeToString(melding))
        }
    }

    private suspend fun hentEndredeUnderenheter(
        endringstype: Endringstype,
        enheter: List<OppdateringDTO>
    ): List<BrregVirksomhetDto> {
        val underenheterFraBrreg = when (endringstype) {
            Endringstype.Sletting,
            Endringstype.Fjernet -> emptyList()
            else -> brregApi.hentUnderenheter(enheter.map { it.organisasjonsnummer })
        }
        return underenheterFraBrreg
    }
}

@Serializable
internal data class OppdateringVirksomhet(
    val oppdateringsid: Long,
    val orgnummer: String,
    val endringstype: Endringstype,
    val metadata: BrregVirksomhetDto? = null,
    val endringstidspunkt: Instant
)


package brreg

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OppdateringService(private val brregApi: BrregApi) {

    suspend fun oppdater() {
        val days = System.getenv("ANTALL_DAGER_SIDEN_OPPDATERING").toLong()
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
        if (endringstype == Endringstype.Fjernet || endringstype == Endringstype.Sletting) {
            println("Disse virksomhetene har endringstype $endringstype: ${Json.encodeToString(enheter)}")
            return
        }
        val data = brregApi.hentUnderenheter(enheter.map { it.organisasjonsnummer })
        println("Endringstype $endringstype data $data")
    }

}

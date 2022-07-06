import brreg.BrregOppdateringDTO
import brreg.Endringstype
import brreg.OppdateringDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object BrregClient {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json = Json(builderAction = {
                ignoreUnknownKeys = true
            }))
        }
    }
}

const val SØKE_STØRRELSE = 100

suspend fun main() {
    val days = System.getenv("ANTALL_DAGER_SIDEN_OPPDATERING").toLong()
    val yesterday = ZonedDateTime.now(ZoneOffset.UTC).minusDays(days)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
    var url =
        "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=${yesterday}&size=$SØKE_STØRRELSE"

    while (url.isNotEmpty()) {
        val response = BrregClient.httpClient.get(url)
        val (_embedded, _links) = response.body<BrregOppdateringDTO>()
        val enheterGruppertPåType = _embedded.oppdaterteUnderenheter.groupBy { it.endringstype }
        enheterGruppertPåType.entries.forEach { (key, value) ->
            sendTilKafka(endringstype = key, enheter = value)
        }
        url = _links.next?.href ?: ""
    }

}

suspend fun sendTilKafka(endringstype: Endringstype, enheter: List<OppdateringDTO>) {
    if (endringstype == Endringstype.Fjernet || endringstype == Endringstype.Sletting) {
        println("Disse virksomhetene har endringstype $endringstype: ${Json.encodeToString(enheter)}")
        return
    }
    val orgnumre = enheter.map { it.organisasjonsnummer }.joinToString(separator = ",")
    val url =
        "https://data.brreg.no/enhetsregisteret/api/underenheter?organisasjonsnummer=$orgnumre&size=${enheter.size}"
    val response = BrregClient.httpClient.get(url)
    val data = response.bodyAsText()
    println("Endringstype $endringstype data $data")
}

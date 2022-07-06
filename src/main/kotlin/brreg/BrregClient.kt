package brreg

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

const val SØKE_STØRRELSE = 100

class BrregClient(engine: HttpClientEngine = CIO.create()) : BrregApi {
    private val httpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json = Json(builderAction = {
                ignoreUnknownKeys = true
            }))
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            println("HttpClient closing...")
            httpClient.close()
        })
    }

    override suspend fun hentOppdaterteUnderenheter(tidspunkt: ZonedDateTime, side: Int): BrregOppdateringDTO {
        val antallDagerSidenSistOppdatering = tidspunkt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
        val url = "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=${antallDagerSidenSistOppdatering}&size=$SØKE_STØRRELSE&page=$side"
        val response = httpClient.get(url)
        return response.body()
    }

    override suspend fun hentUnderenheter(orgnummere: List<String>): String {
        val orgnumreSomString = orgnummere.joinToString(separator = ",")
        val url =
            "https://data.brreg.no/enhetsregisteret/api/underenheter?organisasjonsnummer=$orgnumreSomString&size=${orgnummere.size}"
        val response = httpClient.get(url)
        return response.bodyAsText()
    }
}
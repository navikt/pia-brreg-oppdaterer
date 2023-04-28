package brreg

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

const val SØKE_STØRRELSE = 100

class BrregClient(engine: HttpClientEngine = CIO.create()) : BrregApi {
    private val logger = LoggerFactory.getLogger(this::class.java)
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

    override suspend fun hentOppdaterteUnderenheter(tidspunkt: ZonedDateTime, oppdateringsId: Long?, side: Int): BrregOppdateringDTO {
        val startFilter = if(oppdateringsId != null) {
            "oppdateringsid=$oppdateringsId"
        } else {
            "dato=${tidspunkt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))}"
        }
        val url = "${Miljø.BRREG_OPPDATERING_UNDERENHET_URL}?$startFilter&size=$SØKE_STØRRELSE&page=$side"
        val response = httpClient.get(url)
        val brregOppdateringDTO = try {
            response.body<BrregOppdateringDTO>()
        } catch (e: Exception) {
            logger.warn("Feil ved å hente følgende URL '$url'")
            throw e
        }
        return brregOppdateringDTO
    }

    override suspend fun hentUnderenheter(orgnummere: List<String>): List<BrregVirksomhetDto> {
        val orgnumreSomString = orgnummere.joinToString(separator = ",")
        val url =
            "${Miljø.BRREG_UNDERENHET_URL}?organisasjonsnummer=$orgnumreSomString&size=${orgnummere.size}"
        val response = httpClient.get(url)
        return response.body<BrregUnderenheterResponsDTO>()._embedded.underenheter
    }
}

@Serializable
private data class BrregUnderenheterResponsDTO(
    val _embedded: BrregUnderenheterEmbeddedDTO,
    val _links: LinksDTO,
    val page: PageDTO
)

@Serializable
private data class BrregUnderenheterEmbeddedDTO(val underenheter: List<BrregVirksomhetDto>)

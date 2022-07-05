import brreg.BrregOppdateringDTO
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

suspend fun main() {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json = Json(builderAction = {
                ignoreUnknownKeys = true
            }))
        }
    }
    val yesterday = ZonedDateTime.now( ZoneOffset.UTC).minusDays(1).format( DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") )
    val url = "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=${yesterday}"

    val response = httpClient.get(url)
    val dto = response.body<BrregOppdateringDTO>()
    println(response.headers)
    println(dto)
}
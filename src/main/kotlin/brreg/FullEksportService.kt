package brreg

import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig.Companion.INFINITE_TIMEOUT_MS
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

// Ref: https://data.brreg.no/enhetsregisteret/api/docs/index.html#enheter-lastned
class FullEksportService(
    engine: HttpClientEngine = CIO.create(),
    private val kafkaProdusent: KafkaProdusent,
) {
    companion object {
        val CONTENT_TYPE: String = "application/vnd.brreg.enhetsregisteret.underenhet.v1+gzip;charset=UTF-8"
        var KJØRER_IMPORT = AtomicBoolean(false)
    }

    private val url: String = Miljø.FULL_EKSPORT_URL
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    private val httpClient = HttpClient(engine) {
        install(HttpTimeout) {
            requestTimeoutMillis = INFINITE_TIMEOUT_MS
        }
        install(Logging)
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            println("HttpClient closing...")
            httpClient.close()
        })
    }

    suspend fun lastNed() {
        KJØRER_IMPORT.set(true)
        val brregKomprimert = lagreTilFil(lastNedUnderEnheterSomZip(), "brreg-nedlasting.json.gz")
        val brregUkomprimert = unzipFil(brregKomprimert)
        brregKomprimert.deleteIfExists()
        importerVirksomheterFraFil(brregUkomprimert)
        brregUkomprimert.deleteIfExists()
        KJØRER_IMPORT.set(false)
    }

    private suspend fun lastNedUnderEnheterSomZip(): ByteArray {
        val response = httpClient.get(url) {
            headers {
                append(HttpHeaders.Accept, CONTENT_TYPE)
            }
        }

        return if (response.status.isSuccess()) {
            val bytes = response.readBytes()
            log.info("Lastet ned komprimert fil med størrelse ${bytes.size / 1024 / 1024} MB")
            bytes
        } else {
            val feilmelding = "Kall mot BRREG feilet"
            log.error("Kall mot BRREG feilet. Status: ${response.status}")
            throw IOException(feilmelding)
        }
    }

    private fun importerVirksomheterFraFil(brregUkomprimert: Path) {
        var importerteBedrifter = 0
        var feilendeBedrifter = 0
        log.info("Starter å importere bedrifter fra temporær fil")
        val gson = GsonBuilder().serializeNulls().create()
        JsonReader(InputStreamReader(brregUkomprimert.inputStream())).use { reader ->
            reader.beginArray()
            while (reader.hasNext()) {
                val brregVirksomhet = gson.fromJson<BrregVirksomhetDto>(reader, BrregVirksomhetDto::class.java)
                when (brregVirksomhet) {
                    null -> {
                        feilendeBedrifter++
                        log.debug("Skipper lagring av virksomhet da den er null fra JsonReader")
                    }

                    else -> {
                        try {
                            kafkaProdusent.sendMelding(
                                Miljø.KAFKA_TOPIC_ALLE_VIRKSOMHETER,
                                brregVirksomhet.organisasjonsnummer,
                                Json.encodeToString(brregVirksomhet)
                            )
                            importerteBedrifter++
                        } catch (e: Exception) {
                            feilendeBedrifter++
                            log.error("Lagring av virksomhet feilet", e)
                        }
                    }
                }

                if ((importerteBedrifter + feilendeBedrifter) % 1000 == 0) {
                    log.info("Bedriftsimport fremdrift: Importerte bedrifter: $importerteBedrifter, Feilende bedrifter: $feilendeBedrifter")
                }
            }
            reader.endArray()
        }
        log.info("Bedriftsimport ferdig! Importerte bedrifter: $importerteBedrifter, Feilende bedrifter: $feilendeBedrifter")
    }

    private fun unzipFil(komprimertFil: Path): Path {
        log.info("Unzipper ${komprimertFil.fileName} med størrelse ${komprimertFil.sizeInMb()} MB")
        val ukomprimertFil = createTempFile(prefix = "${System.currentTimeMillis()}", suffix = "brreg-nedlasting.json")
        GZIPInputStream(komprimertFil.inputStream())
            .bufferedReader(UTF_8)
            .useLines { lines ->
                ukomprimertFil.bufferedWriter().use { bw ->
                    lines.forEach {
                        bw.write(it)
                    }
                }
            }
        log.info("Unzippet ${ukomprimertFil.fileName} til størrelse ${ukomprimertFil.sizeInMb()} MB")
        return ukomprimertFil
    }

    private fun lagreTilFil(filinnhold: ByteArray, filnavn: String) =
        createTempFile(
            prefix = "${System.currentTimeMillis()}",
            suffix = filnavn
        ).also { file -> file.writeBytes(filinnhold) }

}


fun Path.sizeInMb() = this.fileSize() / 1024 / 1024

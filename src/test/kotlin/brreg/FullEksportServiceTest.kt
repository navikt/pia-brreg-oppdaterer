package brreg

import brreg.KafkaContainerHelper.Companion.kafkaContainer
import brreg.KafkaContainerHelper.Companion.kafkaKonsument
import brreg.KafkaContainerHelper.Companion.kafkaProducer
import brreg.Miljø.KAFKA_TOPIC_ALLE_VIRKSOMHETER
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

internal class FullEksportServiceTest {

    companion object {
        private lateinit var konsument: KafkaConsumer<String, String>
        private lateinit var mockEngine: HttpClientEngine

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            mockEngine = MockEngine.create {
                addHandler { request ->
                    val path = request.url.encodedPath
                    if (path == "/enhetsregisteret/api/underenheter/lastned") {
                        val virksomheter = listOf(
                            enkeltVirksomhetMock("123456789"),
                            enkeltVirksomhetMock("987654321")
                        )
                        val bos = ByteArrayOutputStream()
                        GZIPOutputStream(bos).bufferedWriter(UTF_8).use {
                            it.write("""
                                [
                                    ${virksomheter.joinToString()}
                                ]
                            """.trimIndent())
                        }
                        respond(
                            bos.toByteArray(),
                            HttpStatusCode.OK,
                            headers {
                                append(HttpHeaders.ContentType, FullEksportService.CONTENT_TYPE)
                            }
                        )
                    } else {
                        respond(status = HttpStatusCode.NotFound, content = "NOT FOUND")
                    }
                }
            }

            konsument = kafkaContainer.kafkaKonsument(klientId = "pia-brreg-alle-virksomheter")
            konsument.subscribe(listOf(KAFKA_TOPIC_ALLE_VIRKSOMHETER))
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            konsument.unsubscribe()
            konsument.close()
        }

        fun enkeltVirksomhetMock(orgnr: String) = """{
            "organisasjonsnummer": "$orgnr",
            "navn": "HESSELBRAND AS",
            "organisasjonsform": {
              "kode": "BEDR",
              "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning",
              "_links": {
                "self": {
                  "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/BEDR"
                }
              }
            },
            "registreringsdatoEnhetsregisteret": "2017-07-08",
            "registrertIMvaregisteret": false,
            "naeringskode1": {
              "beskrivelse": "Arkitekttjenester vedrørende byggverk",
              "kode": "71.112"
            },
            "antallAnsatte": 2,
            "overordnetEnhet": "919202432",
            "oppstartsdato": "2017-06-14",
            "beliggenhetsadresse": {
              "land": "Norge",
              "landkode": "NO",
              "postnummer": "1383",
              "poststed": "ASKER",
              "adresse": [
                "Strøket 8"
              ],
              "kommune": "ASKER",
              "kommunenummer": "3025"
            },
            "_links": {
              "self": {
                "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/919274018"
              },
              "overordnetEnhet": {
                "href": "https://data.brreg.no/enhetsregisteret/api/enheter/919202432"
              }
            }
          }"""

    }


    @Test
    @ExperimentalCoroutinesApi
    fun lastNedAlleVirksomheter() = runTest {
        FullEksportService(mockEngine, kafkaContainer.kafkaProducer(klientId = "pia-brreg-alle-virksomheter")).lastNed()
        withTimeout(Duration.ofSeconds(10)) {
            while (this.isActive) {
                val records = konsument.poll(Duration.ofMillis(100))
                if (!records.isEmpty) {
                    records shouldHaveSize 2
                    records.forEach { record ->
                        val melding = Json.decodeFromString<BrregVirksomhetDto>(record.value())
                        melding.organisasjonsnummer.shouldNotBeNull()
                        melding.naeringskode1?.kode shouldBe "71.112"
                        melding.oppstartsdato shouldMatch "\\d{4}-\\d{2}-\\d{2}"
                    }
                    break
                }
            }
        }
    }
}
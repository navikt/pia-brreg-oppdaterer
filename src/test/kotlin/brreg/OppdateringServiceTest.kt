package brreg

import brreg.KafkaContainerHelper.Companion.kafkaContainer
import brreg.KafkaContainerHelper.Companion.kafkaKonsument
import brreg.KafkaContainerHelper.Companion.kafkaProducer
import brreg.Miljø.KAFKA_TOPIC_OPPDATERINGER
import io.kotest.matchers.collections.shouldContain
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.collections.set

internal class OppdateringServiceTest {

    companion object {
        private lateinit var konsument: KafkaConsumer<String, String>
        private lateinit var mockClient: BrregClient

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            val engine = MockEngine.create {
                addHandler { request ->
                    val path = request.url.encodedPath
                    if (path == "/enhetsregisteret/api/oppdateringer/underenheter") {
                        val json = underenhetOppdateringMock()
                        respond(content = json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    } else if (path == "/enhetsregisteret/api/underenheter") {
                        respond(
                            content = underenheterMock(request.url.encodedQuery),
                            headers = headersOf(HttpHeaders.ContentType, "application/json")
                        )
                    } else {
                        respond(status = HttpStatusCode.NotFound, content = "NOT FOUND")
                    }
                }
            }
            mockClient = BrregClient(engine)

            konsument = kafkaContainer.kafkaKonsument(klientId = "pia-brreg-oppdaterer")
            konsument.subscribe(listOf(KAFKA_TOPIC_OPPDATERINGER))
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            konsument.unsubscribe()
            konsument.close()
        }

        private fun underenheterMock(query: String): String {
            val q = mutableMapOf<String, String>()
            val segmenter = query.split("&")
            segmenter.forEach { segm ->
                val queryString = segm.split("=")
                q[queryString[0]] = queryString[1]
            }
            return """{
      "_embedded": {
        "underenheter": [
          ${q["organisasjonsnummer"]!!.split(",").map { enkeltVirksomhetMock(it) }.joinToString(separator = ",")}
        ]
      },
      "_links": {
        "self": {
          "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/?organisasjonsnummer=919274018"
        }
      },
      "page": {
        "size": ${q["size"]},
        "totalElements": 1,
        "totalPages": 1,
        "number": 0
      }
    }""".trimIndent()
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

        private fun underenhetOppdateringMock() = """{
          "_embedded": {
            "oppdaterteUnderenheter": [
              {
                "oppdateringsid": 14757180,
                "dato": "2022-07-05T04:01:39.613Z",
                "organisasjonsnummer": "927818310",
                "endringstype": "Fjernet",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/927818310"
                  }
                }
              },
              {
                "oppdateringsid": 14757184,
                "dato": "2022-07-05T04:01:39.616Z",
                "organisasjonsnummer": "817704522",
                "endringstype": "Sletting",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/817704522"
                  }
                }
              },
              {
                "oppdateringsid": 14757185,
                "dato": "2022-07-05T04:01:39.616Z",
                "organisasjonsnummer": "997373979",
                "endringstype": "Endring",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/997373979"
                  }
                }
              },
              {
                "oppdateringsid": 14757189,
                "dato": "2022-07-05T04:01:39.625Z",
                "organisasjonsnummer": "926916440",
                "endringstype": "Sletting",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/926916440"
                  }
                }
              },
              {
                "oppdateringsid": 14757191,
                "dato": "2022-07-05T04:02:01.793Z",
                "organisasjonsnummer": "928717844",
                "endringstype": "Sletting",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/928717844"
                  }
                }
              },
              {
                "oppdateringsid": 14757192,
                "dato": "2022-07-05T04:02:01.793Z",
                "organisasjonsnummer": "895699322",
                "endringstype": "Endring",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/895699322"
                  }
                }
              },
              {
                "oppdateringsid": 14757196,
                "dato": "2022-07-05T04:02:01.797Z",
                "organisasjonsnummer": "915250505",
                "endringstype": "Sletting",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/915250505"
                  }
                }
              },
              {
                "oppdateringsid": 14757197,
                "dato": "2022-07-05T04:02:01.797Z",
                "organisasjonsnummer": "924349433",
                "endringstype": "Endring",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/924349433"
                  }
                }
              },
              {
                "oppdateringsid": 14757202,
                "dato": "2022-07-05T04:02:01.805Z",
                "organisasjonsnummer": "919274018",
                "endringstype": "Endring",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/919274018"
                  }
                }
              },
              {
                "oppdateringsid": 14757205,
                "dato": "2022-07-05T04:02:01.808Z",
                "organisasjonsnummer": "915216870",
                "endringstype": "Sletting",
                "_links": {
                  "underenhet": {
                    "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/915216870"
                  }
                }
              }
            ]
          },
          "_links": {
            "first": {
              "href": "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=2022-07-05T00:00:00.000Z&page=0&size=10"
            },
            "self": {
              "href": "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=2022-07-05T00:00:00.000Z&size=10"
            },
            "next": {
              "href": "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=2022-07-05T00:00:00.000Z&page=1&size=10"
            },
            "last": {
              "href": "https://data.brreg.no/enhetsregisteret/api/oppdateringer/underenheter?dato=2022-07-05T00:00:00.000Z&page=89&size=10"
            }
          },
          "page": {
            "size": 10,
            "totalElements": 10,
            "totalPages": 1,
            "number": 0
          }
        }""".trimIndent()
    }

    private val alleEndredeOrgnummere = listOf(
        "997373979",
        "895699322",
        "924349433",
        "919274018",
    )
    private val alleEndredeOppdateringsid = listOf(
        14757185L,
        14757192L,
        14757197L,
        14757202L,
    )
    private val alleFjernedeOrgnummere = listOf(
        "927818310"
    )
    private val alleFjernedeOppdateringsid = listOf(
        14757180L
    )
    private val alleSlettedeOrgnummere = listOf(
        "817704522",
        "926916440",
        "928717844",
        "915250505",
        "915216870",
        "927818310",
    )
    private val alleSlettedeOppdateringsid = listOf(
        14757184L,
        14757189L,
        14757191L,
        14757196L,
        14757205L
    )

    @Test
    @ExperimentalCoroutinesApi
    fun oppdater() = runTest {
        OppdateringService(mockClient, kafkaContainer.kafkaProducer(klientId = "pia-brreg-oppdaterer")).oppdater()
        withTimeout(Duration.ofSeconds(10)) {
            while (this.isActive) {
                val records = konsument.poll(Duration.ofMillis(100))
                if (!records.isEmpty) {
                    records.forEach { record ->
                        val melding = Json.decodeFromString<OppdateringVirksomhet>(record.value())
                        when (melding.endringstype) {
                            Endringstype.Fjernet -> {
                                alleFjernedeOrgnummere shouldContain melding.orgnummer
                                alleFjernedeOppdateringsid shouldContain melding.oppdateringsid
                            }
                            Endringstype.Sletting -> {
                                alleSlettedeOrgnummere shouldContain melding.orgnummer
                                alleSlettedeOppdateringsid shouldContain melding.oppdateringsid
                            }
                            Endringstype.Endring -> {
                                alleEndredeOrgnummere shouldContain melding.orgnummer
                                alleEndredeOppdateringsid shouldContain melding.oppdateringsid
                            }
                            else -> Unit
                        }
                    }
                    break
                }
            }
        }
    }
}
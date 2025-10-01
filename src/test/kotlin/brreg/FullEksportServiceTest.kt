package brreg

import TestContainersHelper
import TestContainersHelper.kafkaContainerHelper
import brreg.Miljø.KAFKA_TOPIC_ALLE_VIRKSOMHETER
import io.kotest.inspectors.forExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.json.Json
import nyKonsument
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.Duration
import kotlin.test.Test

class FullEksportServiceTest {
    private val konsument = kafkaContainerHelper.kafkaContainer.nyKonsument(consumerGroupId = this::class.java.name)

    @BeforeEach
    fun setUp() {
        konsument.subscribe(listOf(KAFKA_TOPIC_ALLE_VIRKSOMHETER))
    }

    @AfterEach
    fun tearDown() {
        konsument.unsubscribe()
        konsument.close()
    }

    @Test
    fun `test full eksport i docker image`() {
        TestContainersHelper.fullEksportContainer.start()
        runBlocking {
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
                        records.toList().forExactly(1) {
                            val melding = Json.decodeFromString<BrregVirksomhetDto>(it.value())
                            melding.beliggenhetsadresse.shouldNotBeNull()
                        }
                        records.toList().forExactly(1) {
                            val melding = Json.decodeFromString<BrregVirksomhetDto>(it.value())
                            melding.postadresse.shouldNotBeNull()
                        }
                        break
                    }
                }
            }
        }
    }
}

package brreg

import TestContainersHelper
import TestContainersHelper.kafkaContainerHelper
import brreg.Miljø.KAFKA_TOPIC_OPPDATERINGER
import io.kotest.matchers.collections.shouldContain
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.withTimeout
import kotlinx.serialization.json.Json
import nyKonsument
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.Duration
import kotlin.test.Test

class OppdateringServiceTest {
    private val konsument = kafkaContainerHelper.kafkaContainer.nyKonsument(consumerGroupId = this::class.java.name)

    @BeforeEach
    fun setUp() {
        konsument.subscribe(listOf(KAFKA_TOPIC_OPPDATERINGER))
    }

    @AfterEach
    fun tearDown() {
        konsument.unsubscribe()
        konsument.close()
    }

    private val alleEndredeOrgnummere = listOf(
        "333333333",
        "666666666",
        "888888888",
        "999999999",
    )
    private val alleEndredeOppdateringsid = listOf(
        14757185L,
        14757192L,
        14757197L,
        14757202L,
    )
    private val alleFjernedeOrgnummere = listOf(
        "111111111"
    )
    private val alleFjernedeOppdateringsid = listOf(
        14757180L
    )
    private val alleSlettedeOrgnummere = listOf(
        "222222222",
        "444444444",
        "555555555",
        "777777777",
        "123456789",
    )
    private val alleSlettedeOppdateringsid = listOf(
        14757184L,
        14757189L,
        14757191L,
        14757196L,
        14757205L,
    )

    @Test
    fun `test oppdatering i docker container`() = runTest {
        TestContainersHelper.oppdateringContainer.start()
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
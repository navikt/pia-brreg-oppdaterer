import brreg.BrregClient
import brreg.KafkaProdusent
import brreg.Miljø
import brreg.OppdateringService

suspend fun main() {
    OppdateringService(
        brregApi = BrregClient(),
        kafkaProdusent = KafkaProdusent(kafkaConfig = Miljø.producerProperties())
    ).oppdater()
}

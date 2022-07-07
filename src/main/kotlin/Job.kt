import brreg.*

suspend fun main() {
    OppdateringService(
        brregApi = BrregClient(),
        kafkaProdusent = DefaultKafkaProdusent(kafkaConfig = Miljø.producerProperties())
    ).oppdater()
}

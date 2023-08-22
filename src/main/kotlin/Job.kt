import brreg.*

suspend fun main() {
    val kafkaProdusent = DefaultKafkaProdusent(kafkaConfig = Miljø.producerProperties())
    when(Miljø.LAST_NED_ALLE_VIRKSOMHETER) {
        "true" -> {
            FullEksportService(
                kafkaProdusent = kafkaProdusent
            ).lastNed()
        }
        else -> {
            OppdateringService(
                brregApi = BrregClient(),
                kafkaProdusent = kafkaProdusent
            ).oppdater()
        }
    }
}

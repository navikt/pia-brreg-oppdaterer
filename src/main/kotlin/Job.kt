import brreg.*

suspend fun main() {
    when(Miljø.LAST_NED_ALLE_VIRKSOMHETER) {
        "true" -> {
            FullEksportService(
                kafkaProdusent = DefaultKafkaProdusent(kafkaConfig = Miljø.producerProperties(klientId = "pia-brreg-alle-virksomheter"))
            ).lastNed()
        }
        else -> {
            OppdateringService(
                brregApi = BrregClient(),
                kafkaProdusent = DefaultKafkaProdusent(kafkaConfig = Miljø.producerProperties(klientId = "pia-brreg-oppdaterer"))
            ).oppdater()
        }
    }
}

package brreg

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.io.Serializable

object Miljø {
    val ANTALL_DAGER_SIDEN_OPPDATERING = getEnvVar("ANTALL_DAGER_SIDEN_OPPDATERING", "1")
    val KAFKA_BROKERS = getEnvVar("KAFKA_BROKERS", "")
    val KAFKA_TRUSTSTORE_PATH = getEnvVar("KAFKA_TRUSTSTORE_PATH", "")
    val KAFKA_KEYSTORE_PATH = getEnvVar("KAFKA_KEYSTORE_PATH", "")
    val KAFKA_CREDSTORE_PASSWORD = getEnvVar("KAFKA_CREDSTORE_PASSWORD", "")
    const val KAFKA_TOPIC = "pia.brreg-oppdatering"

    fun producerProperties(): Map<String, Serializable> {
        val producerConfigs = mutableMapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to KAFKA_BROKERS,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
            ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
            ProducerConfig.CLIENT_ID_CONFIG to "pia-brreg"
        )

        if (KAFKA_TRUSTSTORE_PATH.isNotEmpty()) {
            producerConfigs.putAll(securityConfigs())
        }
        return producerConfigs.toMap()
    }

    private fun securityConfigs() =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to KAFKA_TRUSTSTORE_PATH,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to KAFKA_CREDSTORE_PASSWORD,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to KAFKA_KEYSTORE_PATH,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to KAFKA_CREDSTORE_PASSWORD,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to KAFKA_CREDSTORE_PASSWORD
        )
}


private fun getEnvVar(envVar: String, default: String? = null) =
    System.getenv(envVar) ?: default ?: throw IllegalStateException("Manglende kjøretidsvariabel: $envVar")

package brreg

import brreg.Miljø.KAFKA_TOPIC
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

class KafkaContainerHelper {
    companion object {
        private var adminClient: AdminClient

        val kafkaContainer = KafkaContainer(
            DockerImageName.parse("kymeric/cp-kafka")
                .asCompatibleSubstituteFor("confluentinc/cp-kafka")
        )
            .waitingFor(HostPortWaitStrategy())
            .apply {
                start()
                adminClient = AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers))
                createTopic(KAFKA_TOPIC)
            }

        private fun createTopic(vararg topics: String) {
            val newTopics = topics
                .map { topic -> NewTopic(topic, 1, 1.toShort()) }
            adminClient.createTopics(newTopics)
        }
    }
}

fun KafkaContainer.hentMiljøvariabler() = mapOf(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true, // Den sikrer rekkefølge
    ProducerConfig.ACKS_CONFIG to "all", // Den sikrer at data ikke mistes
    ProducerConfig.CLIENT_ID_CONFIG to "pia-brreg",
)
import brreg.Miljø.KAFKA_TOPIC_ALLE_VIRKSOMHETER
import brreg.Miljø.KAFKA_TOPIC_OPPDATERINGER
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

class KafkaContainerHelper (
    network: Network,
    log: Logger = LoggerFactory.getLogger(KafkaContainerHelper::class.java)
) {
    private var adminClient: AdminClient
    private val kafkaNetworkAlias = "kafkaContainer"

    val kafkaContainer = ConfluentKafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.3")
    )
        .withNetwork(network)
        .withNetworkAliases(kafkaNetworkAlias)
        .withLogConsumer(Slf4jLogConsumer(log).withPrefix(kafkaNetworkAlias).withSeparateOutputStreams())
        .withEnv(
            mapOf(
                "KAFKA_LOG4J_LOGGERS" to "org.apache.kafka.image.loader.MetadataLoader=WARN",
                "KAFKA_AUTO_LEADER_REBALANCE_ENABLE" to "false",
                "KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS" to "1",
                "TZ" to TimeZone.getDefault().id
            )
        )
        .waitingFor(HostPortWaitStrategy())
        .apply {
            start()
            adminClient =
                AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers))
            createTopic(KAFKA_TOPIC_OPPDATERINGER, KAFKA_TOPIC_ALLE_VIRKSOMHETER)
        }

    private fun createTopic(vararg topics: String) {
        val newTopics = topics
            .map { topic -> NewTopic(topic, 1, 1.toShort()) }
        adminClient.createTopics(newTopics)
    }

    fun envVars() = mapOf(
        "KAFKA_BROKERS" to "BROKER://$kafkaNetworkAlias:9093,PLAINTEXT://$kafkaNetworkAlias:9093",
        "KAFKA_TRUSTSTORE_PATH" to "",
        "KAFKA_KEYSTORE_PATH" to "",
        "KAFKA_CREDSTORE_PASSWORD" to "",
    )
}

fun ConfluentKafkaContainer.nyKonsument(consumerGroupId: String) = KafkaConsumer<String, String>(
    mapOf(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers,
        ConsumerConfig.GROUP_ID_CONFIG to consumerGroupId,
        ConsumerConfig.CLIENT_ID_CONFIG to "pia-brreg",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1000",
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "PLAINTEXT",
        SaslConfigs.SASL_MECHANISM to "PLAIN",
    )
)

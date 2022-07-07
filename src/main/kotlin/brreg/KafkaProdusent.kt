package brreg

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

interface KafkaProdusent {
    fun sendMelding(topic: String, nøkkel: String, verdi: String)
}

class DefaultKafkaProdusent(kafkaConfig: Map<String, Any>): KafkaProdusent {
    private val producer: KafkaProducer<String, String> = KafkaProducer(kafkaConfig)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            producer.close()
        })
    }

    override fun sendMelding(topic: String, nøkkel: String, verdi: String) {
        producer.send(ProducerRecord(topic, nøkkel, verdi))
    }
}
package brreg

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaProdusent(kafkaConfig: Map<String, Any>) {
    private val producer: KafkaProducer<String, String> = KafkaProducer(kafkaConfig)

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            producer.close()
        })
    }

    fun sendMelding(topic: String, nøkkel: String, verdi: String) {
        producer.send(ProducerRecord(topic, nøkkel, verdi))
    }
}
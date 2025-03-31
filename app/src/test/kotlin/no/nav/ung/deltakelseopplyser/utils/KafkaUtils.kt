package no.nav.ung.deltakelseopplyser.utils

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.util.*

object KafkaUtils {
    fun EmbeddedKafkaBroker.opprettKafkaProducer(consumerGroupPrefix: String): Producer<String, Any> {
        val producerProps = KafkaTestUtils.producerProps(this)
        producerProps[ProducerConfig.CLIENT_ID_CONFIG] =
            "k9-brukerdialog-producer-$consumerGroupPrefix-${UUID.randomUUID()}"
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            "org.apache.kafka.common.serialization.StringSerializer"
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            "org.apache.kafka.common.serialization.StringSerializer"
        return DefaultKafkaProducerFactory<String, Any>(producerProps).createProducer()
    }

    fun Producer<String, Any>.leggPåTopic(key: String, value: String, topic: String): RecordMetadata {
        return send(ProducerRecord(topic, key, value)).get()
    }

    fun EmbeddedKafkaBroker.opprettKafkaConsumer(
        groupPrefix: String, topics: List<String>,
    ): Consumer<String, String> {
        val consumerProps =
            KafkaTestUtils.consumerProps("$groupPrefix-test-consumer-${UUID.randomUUID()}", "true", this)
        consumerProps[KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        consumerProps[VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"

        val consumer = DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()
        consumer.subscribe(topics)
        return consumer
    }

    fun Consumer<String, String>.lesMelding(key: String, topic: String, maxWaitInSeconds: Long = 60): ConsumerRecord<String, String> {

        val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
        seekToBeginning(assignment())
        while (System.currentTimeMillis() < end) {

            val entries: List<ConsumerRecord<String, String>> = poll(Duration.ofSeconds(10))
                .records(topic)
                .filter { it.key() == key }

            if (entries.isNotEmpty()) {
                Assertions.assertEquals(1, entries.size)
                return entries.first()
            }
        }
        throw IllegalStateException("Fant ikke søknad etter $maxWaitInSeconds sekunder.")
    }
}

package io.learning.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());
    public static void main(String[] args) {
        log.info("I am a Kafka producer!");

        Properties properties = new Properties();
        // Connect to localhost
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");

        // set producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());

        // create a producer
        // Here Key and value is of type string as we defined the serializer to return string in the producer properties
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // Create a producer record
        ProducerRecord<String,String> producerRecord = new ProducerRecord<>("demo_java","hello world1");

        //send data
        producer.send(producerRecord);

        // tell the producer to send all data and block until done
        producer.flush();

        // no need to separately call the producer.flush() as the producer.close call the producer.flush() internally
        producer.close();

    }
}

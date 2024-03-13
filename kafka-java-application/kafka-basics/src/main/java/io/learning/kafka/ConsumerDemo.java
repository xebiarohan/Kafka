package io.learning.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemo {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());
    public static void main(String[] args) {
        log.info("I am a Kafka consumer!");

        String groupId = "my-java-application";
        String topic = "demo_java";

        Properties properties = new Properties();
        // Connect to localhost
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");

        // Consumer configs
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        properties.setProperty("group.id", groupId);

        // Possible values none, earliest and latest
        // none means if the group does not exist then fail
        // earliest means from the beginning
        // latest means the latest messages only
        properties.setProperty("auto.offset.reset", "earliest");


        // create a consumer

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // Subscribe to a topic
        consumer.subscribe(Arrays.asList(topic));

        // poll the data
        while (true) {
            log.info("Polling");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

            for(ConsumerRecord<String, String> consumerRecord: records) {
                log.info("key: " + consumerRecord.key()+ ", Value: " + consumerRecord.value());
                log.info("Partition: " + consumerRecord.partition() + ", Offset: " + consumerRecord.offset());
            }
        }

    }
}

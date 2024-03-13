package io.learning.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoWithCooperative {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemoWithCooperative.class.getSimpleName());
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
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());


        // create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // Get reference to the main thread
        final Thread thread = Thread.currentThread();

        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run(){
                log.info("Detected a shutdown, let's exit using consumer.wakeup()...");
                consumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try {
            // Subscribe to a topic
            consumer.subscribe(Arrays.asList(topic));

            // poll the data
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for(ConsumerRecord<String, String> consumerRecord: records) {
                    log.info("key: " + consumerRecord.key()+ ", Value: " + consumerRecord.value());
                    log.info("Partition: " + consumerRecord.partition() + ", Offset: " + consumerRecord.offset());
                }
            }
        } catch (WakeupException ex) {
            log.info("Consumer is starting to shut down...");
        } catch (Exception ex) {
            log.error("Unexpected error in the consumer", ex);
        } finally {
            consumer.close(); // closes the consumer and this will also commits the offsets
            log.info("Consumer is shut down");
        }


    }
}

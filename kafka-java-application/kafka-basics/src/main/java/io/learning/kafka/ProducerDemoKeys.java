package io.learning.kafka;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemoKeys.class.getSimpleName());
    public static void main(String[] args) {
        log.info("I am a Kafka producer!");

        Properties properties = new Properties();
        // Connect to localhost
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");

        // set producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());

        // Not recommended for production, it sends the messages to different partition in round-robin way
        //properties.setProperty("batch.size", RoundRobinPartitioner.class.getName());

        // create a producer
        // Here Key and value is of type string as we defined the serializer to return string in the producer properties
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        for(int j=0;j<2;j++) {
            for(int i=0;i<10;i++) {

                String topic = "demo_java";
                String key = "id_"+i;
                String value = "Hello world" + i;
                // Create a producer record
                ProducerRecord<String,String> producerRecord = new ProducerRecord<>(topic, key, value);

                //send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        if(e == null) {
                            log.info("key: " + key + "| partition: " + metadata.partition());
                        } else {
                            log.error("Error while producing: " + e.getMessage());
                        }
                    }
                });
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }



        // tell the producer to send all data and block until done
        producer.flush();

        // no need to separately call the producer.flush() as the producer.close call the producer.flush() internally
        producer.close();

    }
}

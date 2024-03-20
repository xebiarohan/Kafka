package org.kafka.wikimedia;

import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.ReadyState;
import com.launchdarkly.eventsource.StreamException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class WikimediaChangesProducer {
    private static Logger log = LoggerFactory.getLogger(WikimediaChangesProducer.class.getSimpleName());
    public static void main(String[] args) throws StreamException, InterruptedException {

        String bootStrapServers = "127.0.0.1:9092";

        // Create producer properties
        Properties properties = new Properties();
        // Connect to localhost
        properties.setProperty("bootstrap.servers", bootStrapServers);

        // set producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());

        // High throughput producer configs
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(32 * 1024)); // 32KB
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");

        // create a producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        String topic = "wikimedia.recentchange";

        String url = "https://stream.wikimedia.org/v2/stream/recentchange";
        EventSource.Builder builder = new EventSource.Builder(URI.create(url));
        EventSource eventSource = builder.build();

        if(eventSource.getState().equals(ReadyState.SHUTDOWN) || eventSource.getState().equals(ReadyState.CLOSED)) {
            producer.close();
        } else {
            eventSource.messages().forEach(message -> {
                log.info(message.getData());
                producer.send(new ProducerRecord<>(topic, message.getData()));
            });

        }
        eventSource.start();

        // We produce for 2 minutes and block the program until then
        TimeUnit.MINUTES.sleep(2);




    }
}

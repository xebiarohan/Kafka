package org.kafka.opensearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.opensearch.client.Requests.INDEX_CONTENT_TYPE;

public class OpenSearchConsumer {

    public static RestHighLevelClient createOpenSearchClient() {
        String connString = "http://localhost:9200";
//        String connString = "https://c9p5mwld41:45zeygn9hy@kafka-course-2322630105.eu-west-1.bonsaisearch.net:443";

        // we build a URI from the connection string
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);
        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null) {
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), "http")));

        } else {
            // REST client with security
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(
                    RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                            .setHttpClientConfigCallback(
                                    httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(cp)
                                            .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));


        }

        return restHighLevelClient;
    }

    public static void main(String[] args) throws IOException {

        Logger logger = LoggerFactory.getLogger(OpenSearchConsumer.class.getSimpleName());

        // Creating OpenSearch client
        RestHighLevelClient openSearchClient = createOpenSearchClient();

        //Creating our Kafka client
        KafkaConsumer<String,String> consumer = createKafkaConsumer();

        // Get reference to the main thread
        final Thread thread = Thread.currentThread();

        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run(){
                logger.info("Detected a shutdown, let's exit using consumer.wakeup()...");
                consumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        try(openSearchClient; consumer) {

            boolean indexExists = openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT);

            if(!indexExists) {
                // We need to create index in OpenSearch if it does not exist
                CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
                openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                logger.info("The wikimedia Index has been created");
            } else {
                logger.info("The Wikimedia Index already exists");
            }

            // Subscribing to a topic
            consumer.subscribe(Collections.singleton("wikimedia.recentchange"));

            while (true) {
                ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(3000));
                int count = records.count();
                logger.info("Received :" + count + " records");

                BulkRequest bulkRequest = new BulkRequest();

                for(ConsumerRecord<String,String> record: records) {
                    try {
                        // id for Idempotent feature
                        // we can extract the unique id from the record also
                        String id = record.topic() + "-" + record.partition() + "-" + record.offset();
                        IndexRequest indexRequest = new IndexRequest("wikimedia")
                                .source(record.value(),INDEX_CONTENT_TYPE)
                                .id(id);

                       // IndexResponse indexResponse = openSearchClient.index(indexRequest, RequestOptions.DEFAULT);
                        //logger.info(indexResponse.getId());

                        bulkRequest.add(indexRequest);
                    } catch (Exception ex) {
                        // DO nothing for the time being
                    }
                }

                if(bulkRequest.numberOfActions() > 0) {
                    BulkResponse response = openSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                    logger.info("Inserted" + response.getItems().length + " records");

                    Thread.sleep(1000);
                }
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (WakeupException ex) {
            logger.info("Consumer is starting to shut down...");
        } catch (Exception ex) {
            logger.error("Unexpected error in the consumer", ex);
        } finally {
            consumer.close(); // closes the consumer and this will also commits the offsets
            openSearchClient.close();
            logger.info("Consumer is shut down");
        }
    }

    private static KafkaConsumer<String, String> createKafkaConsumer() {
        String groupId = "consumer-opensearch-demo";

        Properties properties = new Properties();
        // Connect to localhost
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        // Consumer configs
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");


        // create a consumer

        return new KafkaConsumer<>(properties);
    }
}

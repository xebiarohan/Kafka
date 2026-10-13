

```text
Producer 
  |
  |
  |
  v
kafka producer client library
  |
  |
  |
  v
Brokers
  |
  |
  |
  v
Kafka consumer client library
  |
  |
  |
  v
consumer


```



In Kafka config

1. controller.properties
   - manages cluster metadata, coordinate the leader election for partitions

2. broker.properties
   - Manages the configuration of a server that acts as a broker

3. server.properties
   - Manages configuration of server that acts as both : broker and controller

4. Commands to run a Kafka cluster

```
cd bin
Creating kafka cluster id : kafka-storage.sh random-uuid
Preparing storage directory for server: kafka-storage.sh format -t <cluster-id> -c ../config/server.properties --standalone
Starting kafka server: kafka-server-start.sh ../config/server.properties

```

5. Running multiple servers in a cluster

5.1 Generate the cluster ID and controller IDs

```
CLUSTER_ID=$(./bin/kafka-storage.sh random-uuid)

LOG_ID_1=$(./bin/kafka-storage.sh random-uuid)
LOG_ID_2=$(./bin/kafka-storage.sh random-uuid)
LOG_ID_3=$(./bin/kafka-storage.sh random-uuid)

echo "Cluster ID: $CLUSTER_ID"
echo "Log IDs: '$LOG_ID_1' '$LOG_ID_2' '$LOG_ID_3'"

```

5.2 Format storage directories for all three servers

```
./bin/kafka-storage.sh format \
  -t "$CLUSTER_ID" \
  -c config/server-1.properties \
  --initial-controllers "1@localhost:9093:$LOG_ID_1,2@localhost:9095:$LOG_ID_2,3@localhost:9097:$LOG_ID_3"

./bin/kafka-storage.sh format \
  -t "$CLUSTER_ID" \
  -c config/server-2.properties \
  --initial-controllers "1@localhost:9093:$LOG_ID_1,2@localhost:9095:$LOG_ID_2,3@localhost:9097:$LOG_ID_3"

./bin/kafka-storage.sh format \
  -t "$CLUSTER_ID" \
  -c config/server-3.properties \
  --initial-controllers "1@localhost:9093:$LOG_ID_1,2@localhost:9095:$LOG_ID_2,3@localhost:9097:$LOG_ID_3"

```


5.3 Start all the 3 servers:

```
./bin/kafka-server-start.sh config/server-1.properties
./bin/kafka-server-start.sh config/server-2.properties
./bin/kafka-server-start.sh config/server-3.properties

```

5.4 verify cluster health:

```
./bin/kafka-metadata-quorum.sh --bootstrap-controller localhost:9093  describe --status

```

5.5 stopping Kafka server:
```
./bin/kafka-server.sh  (it will stop all 3 servers)

```


6. Kafka topic CLI

Commands :
```
cd kafka/bin

./kafka-topics.sh --create --topic topic1 --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

./kafka-topics.sh --list --bootstrap-server localhost:9092

./kafka-topics.sh --describe --bootstrap-server localhost:9092

./kafka-topics.sh --delete --topic topic1 --bootstrap-server localhost:9092

```

Good practice is to provide multiple bootstrap-server if present


7. Kafka producer
```
cd bin
./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic topic1

```
Running producer where we can send data with key

```
./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic topic1 --property "parse.key=true" --property "key.separator=:"
```

If a topic is not present, then when we send the first message it will give an error but it will also create that topic. So from second message it will work fine.
This is the default behavior that we can change.
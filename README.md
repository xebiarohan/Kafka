

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
Preparing storage directory for server:

kafka-storage.sh format -t XyBZGrlAT7mgrRF7IcSnkQ -c ../config/server.properties --standalone
Starting kafka server: kafka-server-start.sh ../config/server.properties

```

5. Running multiple servers in a cluster

```
Need to create 1 server.properties file for each node

Create cluster id : CLUSTER_ID=$(./bin/kafka-storage.sh random-uuid)

Generating Log id for each server:

LOG_ID_1=$(./bin/kafka-storage.sh random-uuid)
LOG_ID_2=$(./bin/kafka-storage.sh random-uuid)
LOG_ID_3=$(./bin/kafka-storage.sh random-uuid)
echo "Log IDs: '$LOG_ID_1' '$LOG_ID_2' '$LOG_ID_3'"

Preparing storage directory for servers :
./bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/server-1.properties --initial-controllers "1@localhost:9093:$LOG_ID_1,2@localhost:9095:$LOG_ID_2,3@localhost:9097:$LOG_ID_3"

./bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/server-2.properties --initial-controllers "1@localhost:9093:$LOG_ID_1,2@localhost:9095:$LOG_ID_2,3@localhost:9097:$LOG_ID_3"

./bin/kafka-storage.sh format -t "$CLUSTER_ID" -c config/server-3.properties --initial-controllers
"1@localhost:9093:$LOG_ID_1,2@localhost:9095:$LOG_ID_2,3@localhost:9097:$LOG_ID_3"

Start all the 3 servers:

./bin/kafka-server-start.sh config/server-1.properties
./bin/kafka-server-start.sh config/server-2.properties
./bin/kafka-server-start.sh config/server-3.properties

verify cluster health:

./bin/kafka-metadata-quorum.sh --bootstrap-controller localhost:9093 describe --status

stopping Kafka server:

./bin/kafka-serevr.sh     (it will stop all 3 servers)

```
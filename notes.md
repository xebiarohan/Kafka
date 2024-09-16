1. It decouples the publishers and subscribers

2. Distributed, resilient architecture and fault tolerant

3. Horizontal scalibility
    - can scale to 100s of brokers
    - can scale to millions of messages per second (exmaple in twitter)

4. Topics:
    - A particular stream of data in your Kafka cluster.
    - A cluster can have many topics
    - It is like a table in a database
    - Support any kind of messages (json, binary, text, etc)
    - Sequence of messages is called data stream
    - Can split in partitions (example 100 partitions)
    - Messages in each partition are ordered
    - Each message in a partition gets an id which is called as a Kafka partition offset
    - Data inside a topic is immutable
    - Data is kept for a limited time (By default 1 week - configurable)
    - Order is guaranteed only with in a partition (not across partitions)

5.  Producers
    - Producers write data to topics (which are made of partitions)
    - Producers knows in advance in which partition they have to write to (and which kafka broker has it)
    - In case of Kafka broker failures, producers will automatically recover
    - Producer can choose to send a key with the message (string, number, binary, etc..)
    - If the key is null then the data is passed to partitions in round-robin method
    - If the key is not null, then all the messages with same keys will end up in the same partition (uses hashing strategy)
    - Kafka message atonamy
        - key (can be null)
        - value (can be null)
        - compression type (none, gzip, snappy, lz4, etc)
        - Headers (optional) in key value pair
        - Partition and offset where we want to send the message to
        - Timestamp (system or user set)

6. Kafka message serializer
    - Kafka only accepts bytes as an input from producers and sends bytes out as an output to consumers
    - Message serialization means transforming objects/data into bytes
    - Serialization is used on key and value
    - Can have different serializer for key and value like IntegerSerializer for key and StringSerializer for value
    - There are many types of serializers like String(including JSON), int, float, Avro, etc
    - The serialization/deserialization type must not change during a topic lifecycle (create a new topic instead)

7. Hashing to select the partition
    - Key hashing is the process of determining the mapping of a key to a partition
    - In the default Kafka partitioner, the keys are hashed using the murmur2 algorithm

8. Consumers
    - Consumers read data from a topic (identified by name) - pull model
    - A consumer can consume data from multiple partitions of a topic
    - A consumer knows in advance from which broker, which kafka server to read from (from which partition)
    - Data from each partition is read in order (low to high offset 0,1,2...)
    - Deserializes the key and value from bytes into objects/data

9. Kafka comsumer groups
    - When we have to scale we have many consumers, and all these consumers want to consume data from a topic. So they can be group togather to form a consumer group.
    - It contains multiple consumers
    - Each consumer in the group must read from different partitions from a topic
    - Each consumer group has a unique name
    - If there are more consumers in a group then the partitions in a topic then the remaining consumers remains inactive (stand by consumer)
    - We can have mutiple consumer groups on a topic and consumers from different consumer groups can consume messages from a same partition

10. Consumer offsets
    - Kafka stores the offsets at which a consumer group has been reading
      (keeps track of the partition offset that the consumer already read, So in case of cosumer dies and comes back, the consumer knows how much data already read)
    - The offsets committed are in Kafka topic named __consumer_offsets
    - Consumers periodically commiting the offsets ( the Kafka broker will write to __consumer_offsets, not the group itself)
    - 3 delivery semantics
        - Atleast once
            - Offsets are committed after the message is processed
        - Atmost once
            - offsets are committed as soon as the message is received
            - if processing goes wrong, some messages will be lost (they wont be read again)
        - Exactly once
            - For Kafka - kafka workflows, use the transactional API

11. kafka Broker
    - A kafka cluster is composed of multiple broker
    - A broker is just a server
    - Each broker has a unique id (integer)
    - Each broker contains certain topic partitions
    - Cluster can have over 100 of brokers
    - After connecting to a broker, you will be connected to the whole cluster, means in advance
      you dont need to know about all the brokers in the cluster (explained in next point).

12. Kafka broker discovery
    - Each Kafka broker is also called as bootstrap server
    - Kafka client sends a connection metadata request to one of the brokers
    - Brokers responds with metadata that contains the list of all the brokers
    - then Kafka client connects to the desired broker

13. Topic replication factor
    - In local usually the replication factor is 1 means only leader partition
    - In production it is 2 or 3 (mostly 3) means 1 leader partition and 2 ISR
    - So if a broker goes down then other can take its place (broker that contains its ISR)
    - Example
        - let say we have 3 brokers and 1 topic with replication factory 2
          Broker 101               Broker 102                    Broker 103

      T(partition 0)          T(partition 1)                T(partition 1)
      T(partition 0)

    - If we loose Broker-102, the topic can still serve the data
    - At any time only one broker can be a leader for a partition of a topic
    - Producers will only write to the leader broker
    - Other broker will replicate the written data
    - Each partition can have 1 leader and multiple ISR(in-sync replica)
    - Kafka consumer can only read from the leader of a partition

14. Update in topic replication in Kafka 2.4+
    - Consumer can read for the closest ISR of the partition
    - Closest to the consumer
    - Decreases the latency and the network cost

15. Producer Acknowledgements (acks)
    - Producers can choose to receive acknowledgement of data writes in brokers
    - 3 settings
        - acks=0: Producer won't wait for acknowledgment (possible data loss)
        - acks=1: Producer will wait for leader acknowledgement (limited data loss)
        - acks=-1 or acks=all: Producer wait for leader and all ISR (in-sync replicas) acknowledgment (no data loss)

16. Kafka topic durability
    - For a topic replication factor of N, topic data durability can withstand N-1 brokers loss.

17. Zookeeper
    - Software that manages brokers (keeps a list of them)
    - Helps in performing leader election for partitions
    - Sends notifications to Kafka brokers in case of changes (new topic, broker dies, broker comes up, delete topic, etc)
    - Kafka 2.x can't work without Zookeeper
    - Kafka 3.x can work without Zookeeper(KIP 500) - using Kafka Raft instead
    - Kafka 4.x will not have Zookeeper
    - Works with odd number of servers(1,3,5,7)
    - Has a leader (writes) the rest of the servers are followers (Reads)
    - Does not store consumer offsets with Kafka version > v0.10

18. Kafka KRaft
    - Apache Kafka project to remove Zookeeper dependency from it (KIP 500)
    - Zookeeper shows scaling issues when Kafka clusters have > 100,000 partitions
    - Single security model for the whole system (no zookeeper security)
    - Kafka 3.x now implements the KRaft protocol

19. If we send multiple messages from a producer to a broker then there is a great possibility that they will all go
    to the same partition as a batch. It is called as Sticky partition.

20. Sending to different partitions decreases the performance as we have to send each message in a single batch.

21. Consumer Groups and partition rebalance
    - When even consumer joining, leaving a group or new partition is added to a topic , partition rebalances (reassigned to different consumers of the consumer group)
    - Eager rebalance
        - let say first we have 2 consumers and 3 partitions
        - A new consumer comes
        - First all consumers stops, rejoins the group and gets a new partition assignment randomly
        - So during a small interval, the whole consumer groups stops processing
        - 2 problems: May be the same consumer will not get the same partition (may be that what you want for some reason), second the whole
          consumer is stopped for small interval

    - Cooperative rebalance
        - It does not stops the whole consumer group
        - It reassigs a small subset of partitions from one consumer to another
        - Let say we have 2 consumer and 3 partitions
        - Consumer A has 2 partitions (1 and 2)  and consumer B has 1 partition (3)
        - A new consumer C comes in the group
        - Only consumer A stops and assigns any 1 partition from its list to consumer C
        - Consumer B keeps on consuming from partition 3
    - We select the partition rebalancing strategy using the property : partition.assignment.strategy
    - Default is RangeAssignor (Eager rebalance)
    - Other eager strategies are RoundRobin and StickyAssignor
    - CooperativeStickyAssignor is non eager strategy
    - In kafka connect and Kafka Streams CooperativeStickyAssignor sets as default

22. Static Group Membership
    - By default when a consumer leaves a group, its partitions are revoked and reassigned to other consumers
    - When it joins back, it will get a new Member ID and new partition will be assigned
    - But if we specify "group.instance.id" in the consumer properties then it makes a consumer static member
    - The consumer group will wait for certain amount of time to let consumer to come back with the same id to get the
      same partitions before triggering the reassignment

23. min.insync.replicas producer configuration
    - acks goes hand in hand with another setting that is min.insync.replicas
    - if the acks value is -1 then it will check the value of min.insync.replicas
    - if the value is 1 then it needs an acks from the leader broker only to consider a successful message delivery from a producer
    - if the value is 2 then leader broker and 1 Insync replica needs to acks for successful message delivery and so on..
      In summary when acks=all and the replication.factor is M and min.insync-replicas is N then we can tolerate M-N brokers to go down

24. Producer retries
    - Retry to send a message in case of failure
    - Till Kafka 2.0 the default value is 0
    - from Kafka 2.1 it is a very high number 2147483647
    - retry.backoff.ms default value is 100ms (time interval between retries)

25. Producer timeout
    - As retries has a big number that is limit by the producer timeout
    - delivery.timeout.ms=120000 = 2 minutes
    - Means Retries will happen every 100 ms for 2 minutes (Maximum)
    - timeout consist not only the retries but it gets started when we start batching the message
      to send to a topic
    - It includes baching, await send, retries and Inflight

26. Idempotant producers
    - It will not introduce duplicates on network error
    - Idempotant producers are default from Kafka 3.0
    - It is set by default but if not, we can set it using
        - producerProperties.put("enable.idempotant",true);
          Example
        - Producer sends a request, Kafka commits it and sends a ack
        - Due to network error the ack never reached the producer
        - Producer retries and kafka considers it as a new request and commits it and again sends a new ack

        - But with Idempotant producer
        - When producer retries, the kadka knows that the request is duplicate so does not commits it
          but still sends a ack to the producer.

27. Safe Kafka producer
    - Kafka 3.0 and above are safe by default
    - Safe kafka producer means
        - acks=all
        - min.insyn.replicas = 2 (Must have replication factor of 3 to run this config)
        - enable.idempotant = true (for no duplication)
        - retries = MAX_INT
        - delivery.timeout.ms=120000 (timeout value)
        - max.in.flight.requests.per.connect=5 (ensure max performance while keping message ordered)

28. Message compression at the producer level
    - Decreases the size of the message, smaller message means faster delivery and takes less space on disk
    - It compresses the whole batch (mutliple messages as one) before sending it to kafka
    - It decreases the size so decreaases the latency, time required to publish a batch

29. Message compression at the Broker/topic level
    - We have compression at other places also like at Broker level or at topic level (other than producer)
    - setting
        - compression.type=producer - broker takes the batch and write directly to a topic
        - compression.type=none - all batches are decompressed by the broker
        - compression.type=lz4  (here lz4 is a type of compression)
            - if it matches the producer setting then broker will write it directly to a topic
            - if the compression is different then broker first decompress it and then re-compress it again

30. 2 main settings for Batching messages
    - linger.ms - how long we have to wait before sending a batch (default is 0)
    - batch.size - maximum number of messages (in size) in a batch (default is 16KB)

    let say if the batch.size is 5 and we already got 2 messages in then batch and linger.ms value is 5 then it will wait
    maximum of 5ms to get more messages in the batch before sending the batch to Kafka broker

31. Producer partition
    - When key is null
        - Upto Kafka 2.3 it uses round robin algo to assing messages to partitions
            - In this each message has its own batch because 1 message goes to each partition then again same process
              1,2,3,4,5 again 1,2,3,4,5    (partitions)
        - From 2.4 it uses Sticky-partitioner algo
            - it sticks to a partition until a batch is full or linger.ms time elapsed
            - So a batch of more than 1 message will go to 1 partition

32. Delivery Semantics
    - When the consumer group commits the offsets

    - At most once: offset are committed as soon as the batch is received by any consumer of a consumer group.
      There is a chance that message will be processed. as during processing the consumer can fail

    - Atleast once: offset are committed after the batch is received and completely processed by a consumer of
      a consumer group. Processing needs to be Idempotent otherwise it can cause duplications.

33. Consumer offset commit strategy
    - (easy) enable.auto.commit = true & syncronous process of batches (default)
    - (medium) enable.auto.commit=false & manual commit of offset
    - In Java consumer by default it is atleast-once reading scenario
    - Offsets are committed when you call poll() and auto.commit.interval has elapsed
        - for example if we call poll() every 1 second and auto.commit.interval is 5 seconds
        - then after the fifth call it will commit the consumer offset
    - Make sure the messages are successfully processed before calling poll() again

34. Consumer offset reset behavior
    - In case the consumer goes down then from where it will start reading again
    - Values
        - auto.offset.reset=latest : will read from the end of the log (where it was left before going down)
        - auto.offset.reset=earliest : will read from the beginning
        - auto.offset.reset=none : will throw an error if no offset is found
    - By default offset is maintained for 7 days. This can be updated using
        - offset.retension.minutes

35. Controlling consumer liveliness
    - Consumer in a consumer group coordinates with consumer group coordinator
    - Consumer group coordinator chacks the liveliness of the consumer in a group
    - using the polling mechanism (heartbeat check)
    - default value is heartbeat.interval.ms = 3000 ms
    
    
    
36. Till now we see the Kafka coding that is very low level. With time Kafka evolved and new APIs comes that encapsulates the
    low level code.

37. Kafka Connect
    - It is a source connector to get data from a source and a sink connector to sink data in some data storage
    - It is about the reusability
    - It (Connect Cluster) sits between the source and Kafka cluster and data storage and Kafka cluster
    - Connect cluster comprised of many workers
    - Same Connect Cluster can be used for the soucse connector as well as sink connector
    - There are already lots of connector available (80), so in case of connecting any data source or data storage first check if the connector is already present.

38. Kafka Stream
    - Easy data processing and transformation libreary, it is used for
        - Data transformation
        - Fraud detection    
        - Data enrichment
        - Monitoring and alerting

39. Topic configuration
    - Brokers have default for all the topic configuratiion parameters
    - these parameters impacts the performace and topic behavior
    - Some common topic configurations are:
        - Replication factor
        - Number of partitions
        - Message size
        - Compression level
        - Log cleanup policy
        - Min insync replicas
        - etc

40. Command to create a topic:
    kafka-topics.sh --bootstrap-server localhost:9092 --topic configured-topic --create --partitions 3 --replication-factor 2

41. Command to configure topics
        - this part is common to all the config addition
                kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --alter --add-config
        - adding in sync replica config:
                kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --alter --add-config min.insync.replicas=2   
        - checking added config
                kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --describe     
        - delete a config  
                kafka-configs.sh --bootstrap-server localhost:9092 --entity-type topics --entity-name configured-topic --alter --delete min.insync.replicas 

42. Partitions are segments
    - Topics are made of partitions and partitions are made of segments(files)
    - Each segments have a range of offsets
    - the last segment is known as the active segment, where the data is being written to
    - Two main settings of partitions
        - log.segment.types : the max size of a single segment (default is 1GB)
        - log.segment.ms: the max time kafka wait before committing the segment if not full (1 week)

43. Log cleanup policy
    - Kafka cluster make data expire according to policy called as Log cleanup policy
    - config
        - log.cleanup.policy=delete (default for all user topics) : detete based on age of data( default is 1 week)
        - log.cleanup.policy=compact (will describe later)

44. log.cleanup.policy=delete
        - log.retension.hourse (default is 168 hours - 1 week)
        - log.retension.bytes  - Max bytes for each partition (default is -1 means unlimited)
        - we can keep 2 different types of settings
            - keeping data for 1 week and no limit on size
            - Keeping data for infinite time but delete when the size reaches 500MB

45. log.cleanup.policy=compact
        - in ensures that your log contains atleast the last known value of a specific key in your patition
        - very helpful in case we just need a snapshot insteadd of the full history
        - for example 
            - our topic is employee-salary
            - where key of message  is  the employee name
            - data comes as name-salary
            - for each employee salary will keep on coming with different values
            - After compaction the latest salary of each employee with be left in the segment (topic)
        - deleted record can still be seen by consumer using config
            - delete.retension.ms (default is 24 hours)

46. Large messages in Apache Kafka
    - has a default of 1MB per message in topics, as large messges are considered inefficient and an anti-pattern
    - Two approach for sending large data   
        - Using an external source like S3, Google cloud storage, HDFS and send a reference to that message in Kafka
        - Modifying kafka parameters: must change broker, producer and consumer settings


    

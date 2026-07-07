This is a fundamental Kafka concept. Every message produced to a topic must be written to **exactly one partition**. The producer (specifically, the producer's partitioner) decides which partition to use.

There are several ways this decision is made.

---

# 1. Producer specifies the partition explicitly

The application can tell Kafka exactly which partition to use.

Example:

```java
ProducerRecord<String, String> record =
    new ProducerRecord<>("orders", 2, "key1", "Order Created");
```

Here:

* Topic = `orders`
* Partition = `2`

The message always goes to **Partition 2**.

```text
Orders Topic

P0

P1

P2  ← Message stored here

P3
```

### When is this useful?

* You have complete control over routing.
* Messages for a specific partition have special meaning.
* Advanced use cases (e.g., custom sharding).

---

# 2. Producer specifies a key (most common)

This is the most common approach in real-world applications.

Example:

```java
ProducerRecord<String, String> record =
    new ProducerRecord<>("orders", "Customer123", "Order Created");
```

The producer computes:

```text
hash(key) % number_of_partitions
```

Suppose:

```text
hash("Customer123") = 25
Partitions = 6

25 % 6 = 1
```

The message goes to:

```text
Partition 1
```

Every time the producer sends a message with the key `"Customer123"`, it will map to the same partition (as long as the partition count doesn't change).

---

## Why use a key?

Imagine an e-commerce application.

Messages:

```text
Customer A → Order Created
Customer A → Payment Done
Customer A → Order Shipped
```

All use the key:

```text
CustomerA
```

They all go to the same partition:

```text
Partition 2

Order Created

Payment Done

Order Shipped
```

This preserves the order of events for that customer.

---

# 3. No key, no partition specified

Example:

```java
ProducerRecord<>("orders", "Order Created");
```

No key.

No partition.

In modern Kafka clients, the producer uses the **default partitioner**, which employs a **sticky partitioning** strategy.

Instead of choosing a random partition for every message, it:

* picks one partition,
* keeps sending records to that partition until the current batch is sent,
* then selects another partition for the next batch.

Example:

```text
Batch 1

Message1
Message2
Message3

↓

Partition 0

--------------------

Batch 2

Message4
Message5

↓

Partition 3

--------------------

Batch 3

Message6
Message7

↓

Partition 1
```

This improves batching efficiency and throughput compared with choosing a different partition for every message.

---

# Why not randomly choose every time?

Suppose you have 100 small messages.

Random selection:

```text
P0
P2
P1
P3
P0
P1
P2
```

The producer would have to maintain many partially filled batches.

Sticky partitioning instead keeps messages together for a while:

```text
P2

Message1
Message2
Message3
Message4
Message5
```

The batch fills faster and is sent more efficiently.

---

# What if the number of partitions changes?

Suppose:

```text
hash(Customer123) % 4 = Partition 1
```

Later:

```text
Partitions = 6
```

Now:

```text
hash(Customer123) % 6 = Partition 5
```

The same key may now map to a different partition.

This is why increasing the number of partitions can change key-to-partition mapping and may affect ordering guarantees for future messages with that key.

---

# Can two different keys go to the same partition?

Yes.

Example:

```text
hash(Alice) % 4 = 2

hash(Bob) % 4 = 2
```

Both messages go to:

```text
Partition 2
```

This is normal because different hash values (or values with the same remainder) can map to the same partition.

---

# Custom partitioner

You can also implement your own partitioning logic.

For example:

```text
If country = India
    → Partition 0

If country = USA
    → Partition 1

If country = UAE
    → Partition 2
```

This is useful when your routing logic is based on business rules rather than hashing.

---

# Decision flow

```text
               Send Message
                    │
         ┌──────────┴──────────┐
         │                     │
Partition specified?        No
         │
       Yes
         │
Use that partition
         │
        Done

         No
         │
      Key present?
         │
   ┌─────┴─────┐
   │           │
  Yes         No
   │           │
hash(key)      Sticky partitioner
 % partitions  chooses a partition
   │           │
   └─────┬─────┘
         │
Send to selected partition
```

---

# Interview answer (2–3 minutes)

> **A Kafka producer chooses a partition in three ways. First, the application can explicitly specify the partition, in which case the message is always sent there. Second, if a key is provided but no partition is specified, Kafka hashes the key and computes `hash(key) % number_of_partitions` to determine the destination. This ensures that all messages with the same key are sent to the same partition, preserving their order. Third, if neither a key nor a partition is provided, Kafka's default sticky partitioner selects a partition and continues sending messages there until the current batch is sent, then switches to another partition. This sticky approach improves batching efficiency and overall throughput.

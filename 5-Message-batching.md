Batching is one of the most important performance optimizations in Kafka. Instead of sending every message immediately over the network, the producer groups multiple messages into a **batch** and sends them together.

Let's understand it step by step.

## Why batching?

Imagine your producer sends 10,000 messages.

### Without batching

```
Producer
   |
   |-- Message 1 --> Broker
   |-- Message 2 --> Broker
   |-- Message 3 --> Broker
   |-- Message 4 --> Broker
   ...
```

* 10,000 network requests
* High network overhead
* Lower throughput

---

### With batching

```
Producer

Message1
Message2
Message3
Message4
Message5
    |
    |---- Batch -----> Broker
```

Now instead of sending five separate requests, Kafka sends **one request containing five records**.

Benefits:

* Fewer network calls
* Better compression
* Higher throughput
* Lower CPU usage

---

# How Kafka creates batches

Kafka creates **one batch per partition**.

Suppose your topic has three partitions.

```
Topic Orders

Partition 0
Partition 1
Partition 2
```

Producer sends

```
Order 1 -> Partition 0
Order 2 -> Partition 1
Order 3 -> Partition 0
Order 4 -> Partition 2
Order 5 -> Partition 0
Order 6 -> Partition 1
```

Internally Kafka keeps separate batches.

```
Batch P0

Order1
Order3
Order5

-----------------

Batch P1

Order2
Order6

-----------------

Batch P2

Order4
```

Each partition has its own buffer.

This is important because **Kafka guarantees ordering only within a partition**.

---

# When does Kafka send the batch?

There are two major conditions.

## 1. Batch becomes full (`batch.size`)

Example

```
batch.size = 16 KB
```

Producer keeps filling the batch.

```
Message1
Message2
Message3
...
```

As soon as the batch reaches **16 KB**, Kafka immediately sends it.

Think of it as filling a bucket.

```
Bucket

████████████████

Full

↓

Send
```

---

## 2. Waiting time exceeds `linger.ms`

Suppose

```
batch.size = 16 KB
linger.ms = 10
```

Only three small messages arrive.

```
Message1
Message2
Message3
```

Batch size is only

```
3 KB
```

Kafka waits.

If no more messages arrive within **10 ms**, Kafka sends the partially filled batch.

Otherwise latency would become too high.

---

# Timeline example

Suppose

```
batch.size = 10 messages
linger.ms = 5 ms
```

### Case 1

```
Time 0 ms

Message1
Message2
...
Message10
```

Batch fills instantly.

```
Send immediately
```

No waiting.

---

### Case 2

```
0 ms

Message1

1 ms

Message2

2 ms

Message3
```

Batch is still small.

Kafka waits.

```
5 ms reached

↓

Send batch of 3 messages
```

---

# What if messages keep arriving?

Suppose

```
linger.ms = 20 ms
```

Messages continue arriving.

```
1
2
3
4
5
6
7
8
9
10
```

If the batch fills before 20 ms,

```
Send immediately
```

Kafka does **not** wait for the full `linger.ms` if `batch.size` is reached.

---

# Compression works on batches

Compression happens **after batching**, not per message.

Without batching

```
Message1 -> Compress
Message2 -> Compress
Message3 -> Compress
```

Poor compression ratio.

With batching

```
Message1
Message2
Message3
Message4
Message5

↓

Compress entire batch
```

Much better compression because similar data appears together.

---

# Example configuration

```properties
batch.size=32768
linger.ms=5
compression.type=snappy
```

Meaning:

* Keep up to **32 KB** in one batch.
* Wait up to **5 ms** for more records.
* Compress the entire batch using Snappy before sending.

---

# Does batching affect ordering?

No.

Suppose all these messages go to the same partition.

```
A
B
C
D
E
```

They are stored in the batch exactly in this order.

```
Batch

A
B
C
D
E
```

The broker appends them to the partition log in the same sequence, preserving order.

---

# Memory usage

Before sending, batches are stored in the producer's memory buffer.

```
Producer Memory

+----------------------+
| Batch P0             |
+----------------------+

+----------------------+
| Batch P1             |
+----------------------+

+----------------------+
| Batch P2             |
+----------------------+
```

The total memory available for these buffers is controlled by `buffer.memory`. If the buffer fills up because the producer can't send data fast enough, `send()` may block (up to `max.block.ms`) or eventually fail with an exception.

---

# Real-world example

Imagine an e-commerce application.

A producer receives 1,000 order events per second.

Without batching:

```
1000 HTTP/TCP requests per second
```

With batching (50 messages per batch):

```
20 requests per second

Each request contains 50 records
```

The broker handles far fewer requests, improving throughput while preserving the order of messages within each partition.

---

## Key producer settings related to batching

| Configuration      | Purpose                                                                                    |
| ------------------ | ------------------------------------------------------------------------------------------ |
| `batch.size`       | Maximum size (in bytes) of a batch for a single partition.                                 |
| `linger.ms`        | Maximum time to wait for additional records before sending a not-yet-full batch.           |
| `buffer.memory`    | Total memory available for all unsent batches.                                             |
| `compression.type` | Compresses the entire batch, improving network efficiency.                                 |
| `acks`             | Controls when the broker acknowledges receipt; it affects durability, not batching itself. |

### Interview takeaway

A concise way to explain batching is:

> **The Kafka producer maintains a separate batch for each partition. Messages destined for the same partition are accumulated in memory and sent together either when the batch reaches `batch.size` or when `linger.ms` expires—whichever happens first. Batching reduces network overhead, improves throughput, and enables more effective compression, while preserving the order of messages within each partition.**

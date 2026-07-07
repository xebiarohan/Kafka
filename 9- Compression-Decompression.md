Compression in Kafka is an important optimization because it **reduces network bandwidth, disk usage, and storage costs**. One of Kafka's design principles is that **compression is applied to batches of records**, not to individual messages.

Let's go through it from end to end.

---

# Why do we need compression?

Suppose your producer sends 1 million JSON messages.

Without compression:

```text
Producer
    |
100 MB
    |
Broker
```

The network transfers **100 MB**.

With compression (for example, 4:1 compression):

```text
Producer
    |
25 MB
    |
Broker
```

Benefits:

* Less network traffic
* Faster transfer
* Less disk space
* Better throughput

---

# Kafka Compression Flow

```text
Producer
    │
Batch Messages
    │
Compress Batch
    │
──────── Network ────────
    │
Broker stores compressed batch
    │
Consumer fetches compressed batch
    │
Decompress
    │
Application receives records
```

Notice that **the broker normally stores the compressed batch** instead of decompressing every message.

---

# Producer-level Compression

The producer controls compression using:

```properties
compression.type=gzip
```

or

```properties
compression.type=snappy
```

Supported algorithms include:

* `none`
* `gzip`
* `snappy`
* `lz4`
* `zstd`

---

## Step 1: Producer creates batches

Suppose the producer creates this batch:

```text
Message 1
Message 2
Message 3
Message 4
```

---

## Step 2: Compress the entire batch

Instead of:

```text
Compress M1

Compress M2

Compress M3
```

Kafka performs:

```text
Batch

M1
M2
M3
M4

↓

Compress Entire Batch
```

Compressing a batch yields a much better compression ratio because similar data appears together.

---

## Step 3: Send to Broker

```text
Producer

↓

Compressed Batch

↓

Broker
```

Only the compressed bytes travel over the network.

---

# Broker-side Behavior

A common misconception is that the broker decompresses every message.

In most cases, **it does not**.

Broker receives:

```text
Compressed Batch
```

Stores:

```text
Compressed Batch
```

Writes to disk:

```text
Compressed Batch
```

Replicates:

```text
Compressed Batch
```

The broker simply appends the compressed batch to the log and replicates it to follower brokers.

This makes Kafka extremely efficient.

---

# Consumer-side Decompression

Consumer fetches:

```text
Compressed Batch
```

Kafka client library decompresses it:

```text
Compressed Batch

↓

Decompress

↓

Message1

Message2

Message3
```

Your application receives normal records.

---

# Broker Compression Configuration (Topic Level)

A topic has a configuration called:

```properties
compression.type
```

Possible values:

```text
producer
gzip
snappy
lz4
zstd
uncompressed
```

Let's understand each.

---

## `compression.type=producer` (Default)

This means:

> Keep whatever compression the producer used.

Example:

Producer:

```properties
compression.type=lz4
```

Broker:

```properties
compression.type=producer
```

Result:

```text
Producer

↓

LZ4 Batch

↓

Broker stores LZ4 Batch
```

No recompression occurs.

This is the most efficient option.

---

## Broker forces a compression type

Suppose:

Producer:

```properties
compression.type=gzip
```

Topic:

```properties
compression.type=zstd
```

Flow:

```text
Producer

↓

GZIP

↓

Broker

↓

Decompress

↓

Compress using ZSTD

↓

Store
```

The broker must recompress the data before storing it.

This consumes additional CPU.

---

# Replication

Suppose the leader receives:

```text
Compressed Batch
```

Leader:

```text
Store compressed
```

Follower:

```text
Receive compressed

↓

Store compressed
```

Replication happens on the compressed data, reducing network usage between brokers.

---

# Consumer Does Not Choose Compression

Many beginners think consumers configure compression.

They do not.

Consumer simply fetches the compressed batch and the Kafka client automatically decompresses it.

No configuration is needed to "enable decompression."

---

# Compression Algorithms

| Algorithm | Compression Ratio | Speed          | CPU Usage | Typical Use                       |
| --------- | ----------------- | -------------- | --------- | --------------------------------- |
| none      | None              | Fastest        | Lowest    | Testing or very low latency       |
| snappy    | Medium            | Very fast      | Low       | General-purpose production        |
| lz4       | Medium            | Extremely fast | Low       | Low-latency systems               |
| gzip      | High              | Slower         | Higher    | When storage savings matter most  |
| zstd      | Very high         | Fast           | Moderate  | Modern default for many workloads |

---

# Which algorithm should I choose?

### Snappy

```text
Fast compression

Fast decompression

Good throughput
```

Suitable for most applications.

---

### LZ4

```text
Very fast

Slightly lower compression ratio
```

Ideal for low-latency streaming.

---

### GZIP

```text
Best compression

Higher CPU cost
```

Useful when minimizing storage or bandwidth is more important than CPU.

---

### ZSTD

```text
Excellent compression

Fast decompression

Balanced CPU usage
```

Often the best overall choice on modern Kafka deployments.

---

# End-to-End Example

Producer configuration:

```properties
compression.type=zstd
```

Topic:

```properties
compression.type=producer
```

Flow:

```text
Application

↓

Batch Created

↓

Compress (ZSTD)

↓

Network

↓

Leader Broker

↓

Disk

↓

Follower Broker

↓

Consumer Fetch

↓

Decompress

↓

Application
```

The broker never needs to recompress.

---

# Important Interview Questions

### Q1. Does Kafka compress each message?

**No.**

Kafka compresses **record batches**, not individual messages.

---

### Q2. Where does compression happen?

At the **producer**, before sending the batch.

---

### Q3. Does the broker decompress every message?

Usually **no**.

It stores and replicates compressed batches directly.

The exception is when the topic's `compression.type` forces a different compression algorithm than the one used by the producer. In that case, the broker decompresses and recompresses the batch before storing it.

---

### Q4. Who decompresses the data?

The **Kafka consumer client library** automatically decompresses batches before returning records to your application.

---

# Interview answer (3 minutes)

> **Kafka compresses data at the record batch level rather than per message. The producer first groups messages into a batch, compresses the entire batch using the configured algorithm—such as Snappy, LZ4, GZIP, or Zstandard—and sends the compressed batch to the broker. By default, if the topic's `compression.type` is set to `producer`, the broker stores and replicates that compressed batch without decompressing it, which saves CPU, network bandwidth, and disk space. If the topic is configured with a specific compression algorithm, the broker will decompress the incoming batch and recompress it using the configured algorithm before storing it. When consumers fetch data, the Kafka client library automatically decompresses the batch before delivering individual records to the application. This design makes Kafka highly efficient because compression is performed once per batch rather than once per message.**

# Who compresses the batch Producer or the Broker ?

That's an excellent question, and it's a common point of confusion.

**The short answer is:**

> **The Kafka Producer client compresses the batch**, not your application code and not the broker.

Let's clarify what "producer" means.

There are three components involved:

```text
Your Application
       │
       ▼
Kafka Producer Client Library
       │
       ▼
Kafka Broker
```

When people say **"the producer compresses the batch"**, they mean **the Kafka Producer client library** (for example, the Java `KafkaProducer` class), not the code you write.

---

## Step-by-step flow

Suppose your code is:

```java
ProducerRecord<String, String> record =
    new ProducerRecord<>("orders", "101", "{\"order\":\"created\"}");

producer.send(record);
```

Here's what happens internally:

```text
Your Application
       │
       │ producer.send(record)
       ▼
KafkaProducer
       │
       │ Buffers records
       │
       ▼
Creates a Record Batch
       │
       ▼
Compresses the Batch
       │
       ▼
Sends compressed bytes over TCP
       │
       ▼
Kafka Broker
```

Your application never calls a `compress()` method. The Kafka client does it automatically based on your configuration.

---

## What tells the producer to compress?

This configuration:

```properties
compression.type=gzip
```

or

```properties
compression.type=snappy
```

or

```properties
compression.type=lz4
```

or

```properties
compression.type=zstd
```

When the producer is ready to send a batch, it checks this setting and compresses the batch before transmitting it.

---

## Example

Suppose your producer has collected these messages:

```text
M1
M2
M3
M4
```

Internally, the Kafka Producer client does:

```text
Create Batch

↓

Compress Batch (e.g., Snappy)

↓

Send Compressed Batch
```

The broker simply receives compressed bytes.

---

## Does the broker compress?

Normally, **no**.

If the topic configuration is:

```properties
compression.type=producer
```

(the default)

then:

```text
Producer Client
      │
Compresses Batch
      │
      ▼
Broker
      │
Stores compressed batch as-is
```

The broker does not compress or decompress anything.

---

## When does the broker compress?

Only if the topic is configured with a different compression type.

Example:

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
Producer Client
    │
Compress (GZIP)
    │
    ▼
Broker
    │
Decompress
    │
Compress (ZSTD)
    │
Store
```

This is called **broker-side recompression**.

---

## Interview answer

If asked:

> **Who compresses the batch in Kafka?**

A good answer is:

> **The Kafka Producer client library compresses the record batch before sending it to the broker. The application simply calls `producer.send()`, and the Kafka client automatically creates batches and compresses them according to the `compression.type` configuration. The broker usually stores the compressed batch as-is and only recompresses it if the topic configuration requires a different compression algorithm.**



# How consumer offset works in compressed batches

Excellent question. This gets into **how Kafka stores records internally**.

The key insight is:

> **Offsets are assigned to individual records (messages), not to batches.**
> Compression changes **how records are stored**, but not **how offsets are assigned**.

Let's break it down.

---

### First, what is stored in a partition?

Suppose the producer sends 5 messages.

```text
M1
M2
M3
M4
M5
```

The producer creates one compressed batch:

```text
Batch
------------
M1
M2
M3
M4
M5
------------
```

The broker stores this **single compressed batch** in the partition log.

You might think:

> "If only one compressed batch is stored, how can Kafka know the offset of each message?"

The answer is that **Kafka's batch format contains metadata for every record inside the batch**.

---

### Internally, a batch looks something like this

```text
Compressed Batch

Base Offset = 100

Record 0
Record 1
Record 2
Record 3
Record 4
```

Notice the **Base Offset**.

Kafka doesn't store:

```text
Offset 100
Offset 101
Offset 102
Offset 103
Offset 104
```

explicitly for every record.

Instead, it stores:

```text
Base Offset = 100
```

and each record has a **relative offset** inside the batch.

For example:

| Base Offset | Relative Offset | Actual Offset |
| ----------- | --------------- | ------------- |
| 100         | 0               | 100           |
| 100         | 1               | 101           |
| 100         | 2               | 102           |
| 100         | 3               | 103           |
| 100         | 4               | 104           |

So the offsets are still individual message offsets.

---

### Example

Producer sends:

```text
Order A
Order B
Order C
```

Producer compresses them into one batch.

Broker stores:

```text
Compressed Batch

Base Offset = 25

Record 0
Record 1
Record 2
```

Kafka knows:

```text
Record 0 → Offset 25

Record 1 → Offset 26

Record 2 → Offset 27
```

---

### What happens when the consumer reads?

Suppose the consumer's committed offset is:

```text
26
```

It requests:

> "Give me records starting from offset 26."

Broker looks in the log.

It finds the compressed batch:

```text
Offsets

25
26
27
```

The broker sends **the entire compressed batch** to the consumer.

Why?

Because that's how it's stored.

---

### Does the broker decompress it?

Normally, **no**.

The broker sends:

```text
Compressed Batch
```

The consumer client receives it and decompresses it locally.

Now the consumer has:

```text
Offset 25

Order A

----------------

Offset 26

Order B

----------------

Offset 27

Order C
```

Since it requested offset **26**, it skips the first record and returns:

```text
Order B

Order C
```

to your application.

---

### So why send the whole batch?

Imagine a batch of 100 records.

Consumer asks for:

```text
Offset 157
```

The batch contains:

```text
150

151

152

...

249
```

Kafka sends the entire compressed batch.

The consumer decompresses it.

Then starts returning records from:

```text
157
```

This avoids the broker having to decompress and recompress data for every fetch.

---

# How does the committed offset work?

Suppose the consumer processes:

```text
Offset 100

Offset 101

Offset 102
```

After processing:

```text
commitSync();
```

Kafka stores:

```text
Committed Offset = 103
```

Notice:

It stores **the next record to read**, not a batch number.

---

### Visual example

```
Partition

┌───────────────────────────────┐
│ Compressed Batch              │
│                               │
│ Base Offset = 100             │
│                               │
│ Record Offset 100             │
│ Record Offset 101             │
│ Record Offset 102             │
│ Record Offset 103             │
│                               │
└───────────────────────────────┘

Consumer Offset = 102

↓

Broker sends entire batch

↓

Consumer decompresses

↓

Skips

100

101

↓

Returns

102

103
```

---

### Does compression affect offsets?

**No.**

Compression changes:

* Storage format
* Network transfer
* Disk usage

It does **not** change:

* Offset numbering
* Ordering
* Consumer commits

Offsets are always assigned per record.

---

### An analogy

Think of a ZIP file.

Suppose you have:

```
photos.zip

photo1.jpg

photo2.jpg

photo3.jpg
```

If you want `photo2.jpg`, you download the ZIP file and unzip it.

The ZIP file is like the **compressed batch**.

The individual photos are like **records with offsets**.

The ZIP doesn't change the identity of each photo.

Similarly, a compressed Kafka batch doesn't change the identity (offset) of each message.

---

### Interview answer

> **Even though Kafka stores records as compressed batches, offsets are still assigned to individual records, not to batches. Each batch has a base offset, and every record inside the batch has a relative offset, allowing Kafka to determine the actual offset for each message. When a consumer fetches from a specific offset, the broker typically sends the entire compressed batch containing that offset. The consumer client decompresses the batch locally, skips any earlier records in the batch, and delivers only the records starting from the requested offset. Therefore, compression is transparent to the offset mechanism and does not affect how consumers track or commit progress.**

This is one of Kafka's elegant design decisions: **batching and compression are storage/network optimizations, while offsets remain a logical property of individual records.**


This distinction—**your application vs. the Kafka Producer client vs. the broker**—is often appreciated in interviews because it shows you understand where the work is actually performed.

In Apache Kafka, a **consumer offset** is the position of a consumer within a partition. It tells Kafka **which record the consumer has already processed** and **where it should resume reading next**.

Understanding offsets is essential because Kafka does **not** track consumed messages itself. Instead, **consumers are responsible for managing their own progress** using offsets.

---

# What is an Offset?

Every message written to a Kafka partition gets a unique sequential number called an **offset**.

For example, suppose a partition contains:

| Offset | Message |
| ------ | ------- |
| 0      | Order A |
| 1      | Order B |
| 2      | Order C |
| 3      | Order D |
| 4      | Order E |

Offsets are:

* unique **within a partition**
* monotonically increasing
* never reused

If a consumer has committed **offset = 3**, it means:

* Messages with offsets 0, 1, and 2 are considered processed.
* The next message to read is offset 3.

---

# Why Kafka Uses Offsets

Unlike traditional message queues, Kafka keeps messages for a configured retention period regardless of whether they have been consumed.

This allows:

* replaying old messages
* multiple consumers reading independently
* consumers restarting without data loss
* debugging by rereading historical events

Offsets make all of this possible.

---

# How a Consumer Reads Messages

Suppose a producer writes:

```
Partition 0

Offset 0 -> A
Offset 1 -> B
Offset 2 -> C
Offset 3 -> D
Offset 4 -> E
```

Consumer starts:

```
Current Offset = 0

Read A
Read B
Read C
```

Now consumer commits:

```
Committed Offset = 3
```

This means:

```
Next message to consume = Offset 3 (D)
```

---

# Current Offset vs Committed Offset

There are actually two important offsets.

## Current Position

The consumer has already fetched messages.

Example:

```
Fetched till offset 8
```

Current position:

```
position = 9
```

Meaning:

```
Next fetch starts from 9
```

---

## Committed Offset

This is stored in Kafka.

Example:

```
Committed = 6
```

Meaning:

```
Everything before 6 is acknowledged as processed.
```

If the consumer crashes:

```
Restart begins from offset 6
```

not from 9.

This distinction is very important.

---

# Where Are Offsets Stored?

Kafka stores committed offsets in an internal topic:

```
__consumer_offsets
```

This topic contains:

* consumer group ID
* topic
* partition
* committed offset
* metadata

For example:

```
Consumer Group: payment-service

Topic: orders

Partition: 0

Committed Offset: 1452
```

Kafka automatically manages this topic.

---

# Consumer Groups and Offsets

Offsets belong to a **consumer group**, not to an individual consumer.

Example:

```
Group: analytics

Consumer A
Consumer B
```

Partition assignment:

```
Partition 0 -> Consumer A
Partition 1 -> Consumer B
```

Offsets:

```
Group analytics

Partition 0 = 230

Partition 1 = 891
```

If Consumer A dies:

```
Consumer B takes Partition 0
```

Consumer B starts from offset:

```
230
```

No data is lost.

---

# Auto Offset Commit

Kafka can automatically commit offsets.

Configuration:

```properties
enable.auto.commit=true
auto.commit.interval.ms=5000
```

Every 5 seconds Kafka commits the latest offsets.

Advantages:

* simple
* little code

Disadvantages:

Suppose:

```
Read Message

↓

Auto commit

↓

Application crashes before processing
```

Message is lost because Kafka believes it has already been processed.

---

# Manual Offset Commit

Most production applications use manual commits.

```properties
enable.auto.commit=false
```

Processing flow:

```
Read

↓

Process

↓

Save to Database

↓

Commit Offset
```

If processing fails:

```
No commit

↓

Kafka delivers message again
```

This is much safer.

---

# Synchronous Commit

```java
consumer.commitSync();
```

Behavior:

```
Process record

↓

Commit

↓

Wait for Kafka acknowledgment
```

Advantages:

* reliable
* retries automatically
* guarantees commit success or throws an exception

Disadvantages:

* slower
* blocks the consumer

---

# Asynchronous Commit

```java
consumer.commitAsync();
```

Behavior:

```
Process

↓

Send commit request

↓

Continue consuming
```

Advantages:

* faster
* better throughput

Disadvantages:

Commit may fail silently unless handled with a callback.

---

# Offset Reset Policy

What happens if Kafka has **no committed offset**?

Configuration:

```properties
auto.offset.reset=earliest
```

or

```properties
auto.offset.reset=latest
```

### earliest

Start from beginning.

```
0
1
2
3
4
5
```

Consumer reads everything.

Useful for:

* analytics
* ETL
* batch jobs

---

### latest

Start from end.

Suppose:

```
Current Last Offset = 500
```

Consumer starts at:

```
501
```

Only new messages are consumed.

Useful for:

* live systems
* notifications
* real-time processing

---

### none

Throw an exception if no offset exists.

Useful when you want explicit handling instead of default behavior.

---

# Example of Crash Recovery

Messages:

```
0
1
2
3
4
5
6
```

Consumer:

```
Read 0

Read 1

Read 2

Commit Offset = 3
```

Reads:

```
3

4
```

Crash occurs before committing.

Restart:

Kafka checks:

```
Committed Offset = 3
```

Consumer resumes:

```
3

4

5

6
```

Messages 3 and 4 are processed again.

This provides **at-least-once delivery**.

---

# Offset Commit Timing

Correct order:

```
Poll

↓

Process

↓

Write to DB

↓

Commit Offset
```

Incorrect order:

```
Poll

↓

Commit Offset

↓

Process

↓

Crash
```

The second approach risks message loss because Kafka assumes the message was processed when it wasn't.

---

# Consumer Position Example

Suppose:

```
Offsets

0
1
2
3
4
5
```

Consumer state:

```
Current Position = 6

Committed Offset = 3
```

Meaning:

* The consumer has fetched through offset 5.
* Only messages before offset 3 are durably acknowledged.
* If it crashes now, it will restart from offset 3, reprocessing offsets 3, 4, and 5.

---

# Exactly-Once Processing

Kafka supports **exactly-once semantics** when producers, brokers, and consumers are configured appropriately (for example, using idempotent producers and transactions). In this mode, offset commits can be coordinated with the output of processing so that each input record affects downstream systems exactly once, even across failures.

---

# Best Practices

* Disable auto-commit for applications where correctness matters.
* Commit offsets **only after** successful processing.
* Use `commitSync()` when reliability is more important than latency.
* Use `commitAsync()` when higher throughput is needed and you can tolerate handling occasional commit failures.
* Monitor consumer lag (the difference between the latest partition offset and the consumer group's committed offset) to detect slow consumers.
* Keep processing idempotent when possible, since reprocessing can occur after failures.

### Summary

| Concept                      | Description                                                                    |
| ---------------------------- | ------------------------------------------------------------------------------ |
| Offset                       | Sequential identifier of a record within a partition                           |
| Current Position             | Next offset the consumer will fetch                                            |
| Committed Offset             | Last persisted progress for a consumer group                                   |
| Stored In                    | Internal Kafka topic `__consumer_offsets`                                      |
| Auto Commit                  | Kafka periodically commits offsets automatically                               |
| Manual Commit                | Application commits after successful processing                                |
| `commitSync()`               | Blocking, reliable offset commit                                               |
| `commitAsync()`              | Non-blocking, higher-throughput offset commit                                  |
| `auto.offset.reset=earliest` | Start from the beginning if no committed offset exists                         |
| `auto.offset.reset=latest`   | Start from newly arriving messages if no committed offset exists               |
| Consumer Lag                 | Difference between the latest offset and the consumer group's committed offset |

The key idea is that **offsets represent a consumer group's progress, not the messages themselves**. Kafka retains messages independently of consumption, and by managing offsets, consumers can resume after failures, replay historical data, or coordinate work across multiple consumers in the same group.

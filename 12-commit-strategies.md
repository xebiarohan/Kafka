In distributed messaging systems such as Apache Kafka, an **offset** is the position of a message within a partition. A **consumer offset commit strategy** determines **when a consumer records that it has successfully processed messages**. This affects reliability, performance, and the possibility of duplicate or lost processing.

## Why offset commits matter

Suppose a consumer reads messages with offsets:

```
0, 1, 2, 3, 4
```

If it commits offset `5`, it tells Kafka:

> "I have successfully processed messages up to offset 4. Next time, start from offset 5."

If the consumer crashes before committing, it may reprocess messages. If it commits too early, messages may be lost.

---

## 1. Auto Commit

The simplest strategy.

**How it works**

The Kafka client automatically commits offsets at a fixed interval (default: every 5 seconds when enabled).

```
Read messages
      ↓
Process messages
      ↓
Auto commit occurs every few seconds
```

**Advantages**

* Very easy to configure
* Minimal code
* Suitable for simple applications

**Disadvantages**

* Can lose messages if offsets are committed before processing finishes
* Can produce duplicate processing after failures
* Limited control

**Example**

```
Offsets read:
10
11
12

Auto commit happens

Processing of 12 fails

Restart

Consumer resumes at 13

Message 12 is lost.
```

Best for:

* Logging
* Metrics
* Non-critical workloads

---

## 2. Manual Synchronous Commit (`commitSync()`)

The application explicitly commits offsets after successful processing.

```
Read
   ↓
Process
   ↓
commitSync()
```

The consumer waits until Kafka confirms the commit.

### Advantages

* Strong reliability
* Simple failure semantics
* No message loss due to premature commits

### Disadvantages

* Higher latency
* Reduced throughput because processing waits for commit confirmation

Example:

```
Read offset 20

Process successfully

commitSync()

Next message
```

If the commit fails, the application can retry.

Best for:

* Financial systems
* Orders
* Payment processing

---

## 3. Manual Asynchronous Commit (`commitAsync()`)

The consumer sends the commit request without waiting for Kafka's response.

```
Read
   ↓
Process
   ↓
commitAsync()
Continue immediately
```

### Advantages

* High throughput
* Lower latency
* Better performance

### Disadvantages

* Commit failures are harder to detect
* Commit requests may complete out of order if not handled carefully

Example:

```
Process offsets

100
101
102

Send async commit

Continue processing 103
104
105
```

Best for:

* Analytics
* High-volume streaming
* Event processing

---

## 4. Commit After Processing (Recommended)

Only commit after all records in the batch have been processed successfully.

```
Poll records

↓

Process each record

↓

If everything succeeds

↓

Commit offsets
```

Example:

```
Batch

50
51
52

All processed

Commit offset 53
```

If processing fails at 52:

```
No commit

Restart

Consumer re-reads

50
51
52
```

Duplicates are possible, but no processed messages are skipped.

---

## 5. Per-Record Commit

Commit after every individual message.

```
Read 1

Process

Commit

Read 2

Process

Commit
```

### Advantages

* Minimal duplicate processing after failures

### Disadvantages

* Very poor performance
* High network overhead
* Rarely recommended for high-throughput systems

---

## 6. Batch Commit

Process many records before committing.

```
Poll 500 records

↓

Process all

↓

Commit once
```

Advantages:

* High throughput
* Fewer commit requests
* Better scalability

Trade-off:

If a crash occurs before the commit, the entire batch may be processed again.

---

## 7. Transactional Commit (Exactly-Once Processing)

When using Kafka transactions, producers and consumers can coordinate processing so that outputs and offset commits are committed atomically.

```
Read records

↓

Process

↓

Produce new records

↓

Commit transaction

↓

Offsets committed atomically
```

If the application crashes before the transaction commits:

* Produced records are discarded.
* Offsets are not advanced.
* Messages are reprocessed.

This helps achieve **exactly-once processing** in supported Kafka workflows.

Best for:

* Financial applications
* Banking
* Inventory systems
* Event sourcing pipelines

---

## Comparison

| Strategy      | Reliability | Performance | Duplicate Processing                            | Risk of Message Loss                                                     |
| ------------- | ----------- | ----------- | ----------------------------------------------- | ------------------------------------------------------------------------ |
| Auto Commit   | Low–Medium  | High        | Possible                                        | Possible                                                                 |
| Manual Sync   | High        | Medium      | Possible after crash                            | Low                                                                      |
| Manual Async  | Medium      | High        | Possible                                        | Low if commits occur after processing, but commit failures need handling |
| Per-Record    | Very High   | Low         | Minimal                                         | Very Low                                                                 |
| Batch Commit  | High        | Very High   | Higher after crash                              | Low                                                                      |
| Transactional | Very High   | Medium      | None (within supported transactional workflows) | Very Low                                                                 |

## Choosing a strategy

* **Auto Commit:** Simple applications where occasional duplicates or message loss are acceptable.
* **Manual Synchronous Commit:** When correctness is more important than throughput.
* **Manual Asynchronous Commit:** High-performance applications that can tolerate careful handling of commit failures.
* **Batch Commit:** A common choice for balancing throughput and reliability.
* **Per-Record Commit:** Niche cases where minimizing reprocessing is more important than performance.
* **Transactional Commit:** End-to-end exactly-once processing in Kafka-based pipelines that use transactions.

In practice, many production Kafka consumers use **manual commits after successful batch processing**, as this provides a good balance between performance, operational simplicity, and fault tolerance.

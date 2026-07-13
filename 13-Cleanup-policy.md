In Apache Kafka, the **cleanup policy** determines **what Kafka does with old messages in a topic**. It controls whether records are deleted after some time, retained indefinitely based on keys, or both.

There are two primary cleanup policies:

1. **`delete`** (default)
2. **`compact`**

You can also configure both together (`delete,compact`).

---

## 1. Delete Cleanup Policy (`cleanup.policy=delete`)

This is the default policy.

Kafka retains messages for a configured amount of time or until the log reaches a configured size. After that, old log segments are deleted.

Example:

```
Topic: orders

Offset  Message
0       Order A
1       Order B
2       Order C
3       Order D
```

If the retention period is 7 days:

* After 7 days, Kafka deletes the oldest log segments.
* Consumers that haven't read those messages may no longer be able to access them.

Common related configurations:

```
cleanup.policy=delete

retention.ms=604800000      # 7 days

retention.bytes=1073741824  # 1 GB
```

Use this policy for:

* Event streams
* Logs
* Metrics
* Clickstream data
* Audit events

---

## 2. Compact Cleanup Policy (`cleanup.policy=compact`)

With log compaction, Kafka **keeps the latest record for each message key** rather than every version of a record.

Example:

Producer sends:

| Offset | Key | Value   |
| ------ | --- | ------- |
| 0      | A   | John    |
| 1      | B   | Mike    |
| 2      | A   | Johnny  |
| 3      | C   | Alice   |
| 4      | B   | Michael |

Before compaction:

```
A -> John
B -> Mike
A -> Johnny
C -> Alice
B -> Michael
```

After compaction:

```
A -> Johnny
B -> Michael
C -> Alice
```

Older values for the same key are eventually removed during Kafka's background compaction process.

### Why use compaction?

Suppose Kafka stores user profiles.

```
user1 -> John
user2 -> Mike
user1 -> Johnny
```

A new consumer can read the compacted topic and reconstruct the latest state:

```
user1 -> Johnny
user2 -> Mike
```

This is useful for:

* User profiles
* Account balances
* Product catalogs
* Configuration data
* Caches
* Change Data Capture (CDC)

---

## 3. Delete + Compact (`cleanup.policy=delete,compact`)

Kafka can apply both policies.

Example:

```
cleanup.policy=delete,compact
```

Behavior:

* Kafka keeps only the latest value for each key.
* Records older than the retention period can still be deleted.

This is useful when you want:

* A compacted view of the latest state.
* Old data removed after a certain retention period.

---

## Tombstone Records

In compacted topics, deleting a key is represented by sending a record with a **null value**.

Example:

```
Key: user123
Value: null
```

This is called a **tombstone**.

Kafka keeps the tombstone long enough for consumers and the compaction process to observe it. After the configured tombstone retention period, the tombstone itself can also be removed.

---

## When Does Compaction Happen?

Compaction is **not immediate**.

Kafka runs a background log cleaner that periodically scans log segments and rewrites them, removing obsolete records. Until compaction runs, multiple versions of the same key may still be present in the topic.

---

## Comparison

| Feature                    | Delete                        | Compact                                  |
| -------------------------- | ----------------------------- | ---------------------------------------- |
| Keeps all events           | Yes (until retention expires) | No                                       |
| Keeps latest value per key | No                            | Yes                                      |
| Requires message keys      | No                            | Yes                                      |
| Removes old records        | Based on time/size            | Based on newer records with the same key |
| Good for event history     | Yes                           | No                                       |
| Good for current state     | No                            | Yes                                      |

---

## Example Use Cases

| Use Case                              | Cleanup Policy   |
| ------------------------------------- | ---------------- |
| Order events                          | `delete`         |
| Application logs                      | `delete`         |
| User profile updates                  | `compact`        |
| Product inventory                     | `compact`        |In Apache Kafka, the **cleanup policy** determines **what Kafka does with old messages in a topic**. It controls whether records are deleted after some time, retained indefinitely based on keys, or both.

There are two primary cleanup policies:

1. **`delete`** (default)
2. **`compact`**

You can also configure both together (`delete,compact`).

---

## 1. Delete Cleanup Policy (`cleanup.policy=delete`)

This is the default policy.

Kafka retains messages for a configured amount of time or until the log reaches a configured size. After that, old log segments are deleted.

Example:

```
Topic: orders

Offset  Message
0       Order A
1       Order B
2       Order C
3       Order D
```

If the retention period is 7 days:

* After 7 days, Kafka deletes the oldest log segments.
* Consumers that haven't read those messages may no longer be able to access them.

Common related configurations:

```
cleanup.policy=delete

retention.ms=604800000      # 7 days

retention.bytes=1073741824  # 1 GB
```

Use this policy for:

* Event streams
* Logs
* Metrics
* Clickstream data
* Audit events

---

## 2. Compact Cleanup Policy (`cleanup.policy=compact`)

With log compaction, Kafka **keeps the latest record for each message key** rather than every version of a record.

Example:

Producer sends:

| Offset | Key | Value   |
| ------ | --- | ------- |
| 0      | A   | John    |
| 1      | B   | Mike    |
| 2      | A   | Johnny  |
| 3      | C   | Alice   |
| 4      | B   | Michael |

Before compaction:

```
A -> John
B -> Mike
A -> Johnny
C -> Alice
B -> Michael
```

After compaction:

```
A -> Johnny
B -> Michael
C -> Alice
```

Older values for the same key are eventually removed during Kafka's background compaction process.

### Why use compaction?

Suppose Kafka stores user profiles.

```
user1 -> John
user2 -> Mike
user1 -> Johnny
```

A new consumer can read the compacted topic and reconstruct the latest state:

```
user1 -> Johnny
user2 -> Mike
```

This is useful for:

* User profiles
* Account balances
* Product catalogs
* Configuration data
* Caches
* Change Data Capture (CDC)

---

## 3. Delete + Compact (`cleanup.policy=delete,compact`)

Kafka can apply both policies.

Example:

```
cleanup.policy=delete,compact
```

Behavior:

* Kafka keeps only the latest value for each key.
* Records older than the retention period can still be deleted.

This is useful when you want:

* A compacted view of the latest state.
* Old data removed after a certain retention period.

---

## Tombstone Records

In compacted topics, deleting a key is represented by sending a record with a **null value**.

Example:

```
Key: user123
Value: null
```

This is called a **tombstone**.

Kafka keeps the tombstone long enough for consumers and the compaction process to observe it. After the configured tombstone retention period, the tombstone itself can also be removed.

---

## When Does Compaction Happen?

Compaction is **not immediate**.

Kafka runs a background log cleaner that periodically scans log segments and rewrites them, removing obsolete records. Until compaction runs, multiple versions of the same key may still be present in the topic.

---

## Comparison

| Feature                    | Delete                        | Compact                                  |
| -------------------------- | ----------------------------- | ---------------------------------------- |
| Keeps all events           | Yes (until retention expires) | No                                       |
| Keeps latest value per key | No                            | Yes                                      |
| Requires message keys      | No                            | Yes                                      |
| Removes old records        | Based on time/size            | Based on newer records with the same key |
| Good for event history     | Yes                           | No                                       |
| Good for current state     | No                            | Yes                                      |

---

## Example Use Cases

| Use Case                              | Cleanup Policy   |
| ------------------------------------- | ---------------- |
| Order events                          | `delete`         |
| Application logs                      | `delete`         |
| User profile updates                  | `compact`        |
| Product inventory                     | `compact`        |
| CDC streams (e.g., from databases)    | `compact`        |
| Event sourcing with limited retention | `delete,compact` |

### Summary

* **`delete`** retains messages for a configured time or size limit, then removes old log segments.
* **`compact`** retains the latest record for each key, making it possible to reconstruct the current state of the data.
* **`delete,compact`** combines both behaviors: it preserves the latest value for each key while also enforcing retention limits on older data.

| CDC streams (e.g., from databases)    | `compact`        |
| Event sourcing with limited retention | `delete,compact` |

### Summary

* **`delete`** retains messages for a configured time or size limit, then removes old log segments.
* **`compact`** retains the latest record for each key, making it possible to reconstruct the current state of the data.
* **`delete,compact`** combines both behaviors: it preserves the latest value for each key while also enforcing retention limits on older data.

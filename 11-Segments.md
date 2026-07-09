A **segment** is one of the most fundamental storage concepts in Kafka. Understanding it also explains **why Kafka can handle terabytes of data efficiently**.

The short definition is:

> **A segment is a physical log file on disk that stores a portion of a partition's messages. A partition is made up of one or more segment files.**

Let's understand why Kafka needs segments.

---

# Why doesn't Kafka store everything in one file?

Suppose a topic receives:

* 5 million messages/day
* 2 KB/message

That's about **10 GB/day**.

After one year:

```text
3650 GB
```

If Kafka stored all messages in a single file:

```text
Partition

---------------------------------------------------------
4 TB Log File
---------------------------------------------------------
```

Problems:

* File becomes enormous.
* Deleting old data is expensive.
* Searching and recovery become slower.
* Operating systems handle many moderately sized files better than one huge file.

So Kafka divides a partition into **segments**.

---

# Partition = Collection of Segments

Suppose a partition contains 12 messages.

Instead of:

```text
Partition

M1
M2
M3
...
M12
```

Kafka stores:

```text
Partition

Segment-1
Segment-2
Segment-3
```

For example:

```text
Partition 0

+----------------+
| Segment 000000 |
+----------------+
| M1             |
| M2             |
| M3             |
| M4             |
+----------------+

+----------------+
| Segment 000004 |
+----------------+
| M5             |
| M6             |
| M7             |
| M8             |
+----------------+

+----------------+
| Segment 000008 |
+----------------+
| M9             |
| M10            |
| M11            |
| M12            |
+----------------+
```

Each segment contains a consecutive range of offsets.

---

# Segment Naming

Kafka names segment files using the **base offset** of the first message in the segment.

Example:

```text
00000000000000000000.log

00000000000000001000.log

00000000000000002000.log
```

The first file starts at offset:

```text
0
```

The second starts at:

```text
1000
```

The third starts at:

```text
2000
```

This makes it easy to locate the correct segment for a requested offset.

---

# What's inside a segment?

Each segment actually consists of multiple files.

Suppose the segment starts at offset 1000.

Kafka creates files like:

```text
1000.log
1000.index
1000.timeindex
1000.snapshot   (transaction state if needed)
```

### `1000.log`

Contains the actual record batches.

```text
Offset 1000

Order A

Offset 1001

Order B

Offset 1002

Order C
```

---

### `1000.index`

Maps offsets to positions within the `.log` file.

Example:

| Offset | Position in Log |
| ------ | --------------- |
| 1000   | Byte 0          |
| 1010   | Byte 512        |
| 1020   | Byte 1024       |

Kafka uses this to quickly locate records.

---

### `1000.timeindex`

Maps timestamps to offsets.

Example:

| Timestamp | Offset |
| --------- | ------ |
| 10:00     | 1000   |
| 10:10     | 1100   |
| 10:20     | 1200   |

Useful when consumers ask for records starting at a particular timestamp.

---

# Active Segment vs Inactive Segments

Suppose:

```text
Partition

Segment A

Segment B

Segment C
```

Only the **last segment** is writable.

```text
Segment A

Read Only

------------------

Segment B

Read Only

------------------

Segment C

Read + Write
```

This is called the **active segment**.

All earlier segments are immutable.

---

# When is a new segment created?

Kafka "rolls" to a new segment when configured limits are reached.

### Based on size

```properties
log.segment.bytes=1GB
```

When the active segment reaches 1 GB:

```text
Segment Full

↓

Close Segment

↓

Create New Segment
```

---

### Based on time

```properties
log.roll.ms=3600000
```

After one hour:

```text
Current Segment

↓

Close

↓

New Segment
```

Even if it isn't full.

---

# Why immutable segments?

Suppose messages are stored like this:

```text
Segment

100
101
102
103
104
```

Once closed:

```text
Read Only
```

Advantages:

* No locking for old data
* Fast replication
* Simple recovery
* Efficient caching
* Easy deletion

---

# Log Retention

Suppose retention is:

```properties
retention.ms=7 days
```

Partition:

```text
Segment 1

1 week old

---------------

Segment 2

5 days old

---------------

Segment 3

Today
```

Kafka deletes:

```text
Segment 1
```

instead of deleting individual messages.

This is why Kafka retention is so efficient.

---

# Compaction

Suppose the topic is log-compacted.

Segment:

```text
User1 -> A

User2 -> X

User1 -> B

User2 -> Y
```

After compaction:

```text
User1 -> B

User2 -> Y
```

Compaction operates at the segment level, rewriting eligible closed segments while leaving the active segment untouched.

---

# Reading a Record

Suppose a consumer requests:

```text
Offset = 2350
```

Kafka:

Step 1

Find the segment.

```text
Segment

2000.log
```

Step 2

Use the index.

```text
2350

↓

Byte Position
```

Step 3

Jump directly to that location.

Kafka doesn't scan the whole partition.

---

# Real Directory Structure

On disk, a partition might look like:

```text
orders-0/

00000000000000000000.log
00000000000000000000.index
00000000000000000000.timeindex

00000000000000001000.log
00000000000000001000.index
00000000000000001000.timeindex

00000000000000002000.log
00000000000000002000.index
00000000000000002000.timeindex
```

Each group of files represents one segment.

---

# Why segments make Kafka fast

Without segments:

```text
4 TB File

↓

Delete Old Messages

↓

Very Slow
```

With segments:

```text
Segment1

Segment2

Segment3

↓

Delete Segment1

Done
```

No rewriting of the remaining data is needed.

---

# Segment Lifecycle

```text
Producer Writes

        │

        ▼

Active Segment

        │

(Size/Time Limit Reached)

        ▼

Close Segment

        │

Create New Active Segment

        │

Retention or Compaction

        ▼

Delete or Rewrite Old Closed Segments
```

---

# Interview Questions

### Is a partition a file?

**No.**

A partition is a **logical append-only log** that is stored as **multiple segment files** on disk.

---

### Can multiple segments be written simultaneously?

**No.**

Only the **active segment** accepts new writes.

Older segments are read-only.

---

### Why does Kafka use segments?

To:

* Avoid huge files
* Enable fast retention
* Improve recovery
* Support efficient indexing
* Simplify replication
* Improve operating system file management

---

# Interview answer (3–4 minutes)

> **A Kafka partition is not stored as one large file. Instead, it is divided into multiple segment files, where each segment contains a consecutive range of message offsets. Each segment consists of a `.log` file containing the actual record batches, an `.index` file that maps offsets to byte positions, and a `.timeindex` file that maps timestamps to offsets. Kafka writes new records only to the active segment. When the active segment reaches a configured size (`log.segment.bytes`) or age (`log.roll.ms`), Kafka closes it and creates a new active segment. Because closed segments are immutable, Kafka can efficiently delete entire segments for retention or rewrite them during log compaction. This segmented design allows Kafka to scale to very large logs while providing fast lookups, efficient recovery, and high-performance storage management.

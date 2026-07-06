Partition rebalancing is one of the most important concepts in Kafka consumer groups. Understanding it well helps explain how Kafka achieves scalability and fault tolerance.

---

# What is Partition Rebalancing?

Partition rebalancing is the process of **redistributing partitions among consumers in the same consumer group** whenever the membership of the group changes.

These changes can happen when:

* A new consumer joins the group
* A consumer leaves the group
* A consumer crashes
* The number of partitions changes
* A consumer stops responding (session timeout)

The goal is to ensure:

* Every partition is assigned to exactly one consumer within a group.
* The workload is distributed as evenly as possible.

---

# Example

Suppose we have a topic with 6 partitions.

```text
Orders Topic

P0
P1
P2
P3
P4
P5
```

Initially, there are 2 consumers.

```text
Consumer Group

Consumer A
Consumer B
```

Kafka assigns partitions like this:

```text
Consumer A

P0
P1
P2

---------------------

Consumer B

P3
P4
P5
```

Everything is balanced.

---

# A new consumer joins

Now Consumer C starts.

```text
Consumer A
Consumer B
Consumer C
```

Kafka cannot simply give new partitions to Consumer C because all partitions are already assigned.

So Kafka performs a **rebalance**.

New assignment:

```text
Consumer A

P0
P1

---------------

Consumer B

P2
P3

---------------

Consumer C

P4
P5
```

Now all consumers have work.

---

# Consumer crashes

Suppose Consumer B crashes.

Before crash:

```text
A -> P0 P1

B -> P2 P3

C -> P4 P5
```

After rebalance:

```text
A -> P0 P1 P2

C -> P3 P4 P5
```

The partitions owned by B are redistributed.

---

# Why is rebalancing expensive?

During a rebalance, consumers generally pause consuming while the group coordinator computes new assignments and consumers revoke and acquire partitions.

Imagine thousands of partitions.

```text
Consumer A

Processing...

↓

Stop

↓

Revoke partitions

↓

Receive new assignment

↓

Resume
```

During this time:

* Consumption pauses
* Throughput decreases
* Latency increases

This is why Kafka introduced improved rebalance strategies over time.

---

# Rebalancing Strategies (Partition Assignors)

Kafka provides several built-in partition assignment strategies.

1. Range Assignor
2. Round Robin Assignor
3. Sticky Assignor
4. Cooperative Sticky Assignor

Let's understand each.

---

# 1. Range Assignor

This is one of Kafka's oldest strategies.

It assigns **contiguous ranges of partitions** to consumers.

Example:

Topic has 8 partitions.

```text
P0
P1
P2
P3
P4
P5
P6
P7
```

Two consumers:

```text
Consumer A

Consumer B
```

Assignment:

```text
Consumer A

P0
P1
P2
P3

----------------

Consumer B

P4
P5
P6
P7
```

Advantages:

* Simple
* Preserves locality

Disadvantages:

Not always balanced when subscribing to multiple topics or when partitions aren't evenly divisible.

Example:

5 partitions

2 consumers

```text
A -> P0 P1 P2

B -> P3 P4
```

A has more work.

---

# Multiple topic problem

Topic A

```text
P0
P1
P2
```

Topic B

```text
P0
P1
P2
```

Consumers

```text
A
B
```

Range assignor may produce:

```text
Consumer A

TopicA P0 P1

TopicB P0 P1

------------------

Consumer B

TopicA P2

TopicB P2
```

Consumer A receives more partitions.

---

# 2. Round Robin Assignor

Instead of assigning ranges, Kafka distributes partitions one by one.

Example:

Partitions

```text
P0
P1
P2
P3
P4
P5
```

Consumers

```text
A

B

C
```

Assignment:

```text
A -> P0 P3

B -> P1 P4

C -> P2 P5
```

Much more balanced.

Advantages:

* Better load balancing
* Fair distribution

Disadvantages:

When consumers subscribe to different topics, balancing can become uneven or some consumers may receive no partitions for topics they don't subscribe to.

---

# 3. Sticky Assignor

Introduced to reduce unnecessary partition movement.

Goal:

* Balance partitions
* Move as few partitions as possible

Example

Initially

```text
A

P0
P1
P2

------------

B

P3
P4
P5
```

Consumer C joins.

Round Robin might reshuffle almost everything.

Sticky tries to keep existing assignments.

Possible result:

```text
A

P0
P1

--------------

B

P3
P4

--------------

C

P2
P5
```

Only two partitions moved.

Advantages:

* Less data movement
* Less cache invalidation
* Faster recovery
* Lower rebalance cost

This is preferred over Range in many cases.

---

# 4. Cooperative Sticky Assignor

Introduced to support **incremental cooperative rebalancing**.

This is currently the recommended strategy for many modern applications.

The key idea is:

**Don't revoke all partitions immediately.**

Instead:

* Keep processing partitions that don't need to move.
* Revoke only the partitions that must change ownership.
* Complete the reassignment over one or more rebalance rounds.

---

Traditional rebalance

```text
Consumer A

Stop everything

↓

Release all partitions

↓

Wait

↓

Receive new partitions

↓

Resume
```

Lots of interruption.

---

Cooperative rebalance

```text
Consumer A

Keep P0
Keep P1

↓

Release only P2

↓

Continue processing
```

Much smaller pause.

Advantages:

* Minimal downtime
* Much smaller pauses
* Better throughput
* Ideal for large production clusters

---

# Comparison

| Strategy           | Balanced  | Minimizes Movement | Pause During Rebalance | Typical Use                                       |
| ------------------ | --------- | ------------------ | ---------------------- | ------------------------------------------------- |
| Range              | Fair      | No                 | High                   | Simple workloads, single-topic consumers          |
| Round Robin        | Very good | No                 | High                   | Even load distribution                            |
| Sticky             | Very good | Yes                | Moderate               | General-purpose applications                      |
| Cooperative Sticky | Very good | Yes                | Low                    | Modern production systems; often the best default |

---

# Which strategy should you use?

* **Range**: Fine for simple applications where each consumer subscribes to a single topic.
* **Round Robin**: Good if you mainly care about evenly distributing work.
* **Sticky**: Better than the previous two when you want to avoid unnecessary partition movement.
* **Cooperative Sticky**: The preferred choice for most modern Kafka deployments because it minimizes disruption during rebalances.

---

# How to configure the strategy

In the consumer configuration:

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

You can also provide multiple assignors (ordered by preference), for example:

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor,org.apache.kafka.clients.consumer.StickyAssignor
```

The consumer group coordinator selects an assignor that is supported by all members of the group.

---

# Interview answer (2–3 minutes)

> **Partition rebalancing is the process by which Kafka redistributes topic partitions among consumers in the same consumer group whenever group membership or subscribed partitions change. During a rebalance, Kafka ensures that each partition is assigned to exactly one consumer in the group. Traditional strategies like Range and Round Robin can cause all consumers to pause and may move many partitions unnecessarily. Sticky Assignor improves this by keeping existing assignments whenever possible while still balancing the load. Cooperative Sticky Assignor goes a step further by performing incremental rebalancing—only the partitions that actually need to move are revoked and reassigned—allowing consumers to continue processing their other partitions with minimal interruption. This reduces pause times and makes it the preferred strategy for most modern Kafka applications.**

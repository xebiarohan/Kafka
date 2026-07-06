The role of **ZooKeeper** in Kafka depends on the Kafka version you're talking about.

* **Older Kafka versions (before KRaft):** ZooKeeper was a critical dependency.
* **Modern Kafka (Kafka 4.0+ and Kafka 3.x with KRaft):** ZooKeeper has been removed and replaced by Kafka's built-in **KRaft (Kafka Raft)** metadata management.

Since many interviews and legacy systems still use ZooKeeper-based Kafka, it's useful to understand how it worked.

---

# What is ZooKeeper?

**ZooKeeper** is a distributed coordination service used by distributed systems to maintain shared configuration and coordinate cluster state.

Kafka used ZooKeeper to maintain cluster metadata and coordinate brokers.

> ZooKeeper **did not store Kafka messages**. Messages were always stored on Kafka brokers.

---

# Why was ZooKeeper needed?

Imagine a Kafka cluster with multiple brokers.

```
Broker 1
Broker 2
Broker 3
Broker 4
```

Questions Kafka needs to answer include:

* Which brokers are alive?
* Which broker is the controller?
* Which broker is the leader of Partition 0?
* Which broker is the follower?
* Which topics exist?
* What is the replication factor?
* Which broker joined?
* Which broker crashed?

ZooKeeper maintained this shared state.

---

# Architecture

```
                +------------------+
                |    ZooKeeper     |
                | Metadata Store   |
                +---------+--------+
                          |
        ----------------------------------------
        |                |                    |
   Broker 1         Broker 2            Broker 3
```

Every broker connects to ZooKeeper.

Clients generally connect only to Kafka brokers, not to ZooKeeper.

---

# Responsibilities of ZooKeeper

## 1. Broker Registration

When a Kafka broker starts:

```
Broker 1 starts
```

It registers itself in ZooKeeper.

ZooKeeper stores information like:

```
Broker ID = 1

Host = broker1

Port = 9092
```

If Broker 2 starts:

```
Broker ID = 2
Host = broker2
```

ZooKeeper now knows the cluster contains:

```
Broker 1

Broker 2

Broker 3
```

---

## 2. Broker Discovery

ZooKeeper maintains the list of all active brokers.

When a broker joins or leaves, ZooKeeper updates the cluster state.

Example:

Initially

```
Broker1
Broker2
Broker3
```

Broker4 starts

ZooKeeper updates

```
Broker1
Broker2
Broker3
Broker4
```

---

## 3. Controller Election

One Kafka broker acts as the **controller**.

The controller is responsible for cluster-wide operations such as:

* leader election
* partition reassignment
* broker failure handling
* topic creation/deletion

Example

```
Broker1
Broker2
Broker3
```

ZooKeeper elects

```
Controller = Broker2
```

If Broker2 crashes

ZooKeeper detects it.

A new controller is elected.

```
Controller = Broker1
```

---

## 4. Leader Election for Partitions

Suppose

```
Topic Orders

Partition 0
```

Replicated across

```
Broker1

Broker2

Broker3
```

One replica becomes the leader.

```
Leader

Broker2

Followers

Broker1

Broker3
```

If Broker2 crashes

ZooKeeper informs the controller.

The controller elects a new leader.

```
Broker1 becomes leader
```

Clients now produce and consume from Broker1.

---

## 5. Failure Detection

ZooKeeper uses **ephemeral nodes**.

Each broker periodically sends heartbeats to ZooKeeper.

```
Broker1
heartbeat

Broker2
heartbeat

Broker3
heartbeat
```

If Broker2 stops sending heartbeats

ZooKeeper automatically removes its ephemeral node.

The controller receives notification.

Recovery starts.

---

## 6. Topic Metadata

ZooKeeper stored metadata such as:

* Topic name
* Number of partitions
* Replication factor
* Replica assignments

Example

```
Orders

Partitions = 6

Replication Factor = 3
```

---

## 7. Partition Assignment

Suppose

```
Orders Topic

6 partitions

3 brokers
```

ZooKeeper stores

```
P0 → Broker1

P1 → Broker2

P2 → Broker3

...
```

---

## 8. Configuration Storage

ZooKeeper stored configuration such as:

* broker configurations
* quotas
* ACLs
* topic configurations

---

## 9. Cluster Membership

ZooKeeper always knows

```
Broker1 Alive

Broker2 Alive

Broker3 Down

Broker4 Alive
```

Kafka brokers continuously watch ZooKeeper for changes.

---

# What ZooKeeper did NOT do

ZooKeeper never stored Kafka messages.

Example

```
Producer

↓

Broker

↓

Partition

↓

Messages
```

Messages are stored only on Kafka brokers.

ZooKeeper stores only metadata.

---

# Example Flow

Assume

```
Topic

Orders

Partitions = 3

Replication = 3
```

Cluster

```
Broker1

Broker2

Broker3
```

### Step 1

All brokers register with ZooKeeper.

```
ZooKeeper

Broker1

Broker2

Broker3
```

---

### Step 2

ZooKeeper elects

```
Controller

Broker2
```

---

### Step 3

Controller assigns leaders

```
P0 → Broker1

P1 → Broker2

P2 → Broker3
```

---

### Step 4

Producer wants to send data

```
Producer

↓

Broker2 (bootstrap)

↓

Metadata

↓

Leader = Broker1

↓

Producer sends data to Broker1
```

ZooKeeper is not involved in the data transfer.

---

### Step 5

Broker1 crashes

ZooKeeper notices Broker1's heartbeat has stopped.

ZooKeeper informs the controller.

The controller elects a new leader.

```
P0

Leader

Broker2
```

Clients refresh metadata and continue working.

---

# Why Kafka Removed ZooKeeper

ZooKeeper worked well but introduced operational complexity:

* An extra distributed system to deploy and maintain.
* Additional network communication between brokers and ZooKeeper.
* Scalability limitations for very large clusters.
* More moving parts, making operations and upgrades harder.

To simplify the architecture, Kafka introduced **KRaft (Kafka Raft)**. In KRaft mode:

* Kafka brokers themselves manage cluster metadata using the **Raft consensus algorithm**.
* There is no external ZooKeeper dependency.
* Metadata management becomes faster, simpler, and easier to operate.

---

# Summary

| Responsibility                | ZooKeeper (Legacy Kafka)                                    |
| ----------------------------- | ----------------------------------------------------------- |
| Broker registration           | ✔️                                                          |
| Broker discovery              | ✔️                                                          |
| Controller election           | ✔️                                                          |
| Leader election               | ✔️ (controller performs the election using ZooKeeper state) |
| Failure detection             | ✔️                                                          |
| Cluster metadata storage      | ✔️                                                          |
| Topic metadata                | ✔️                                                          |
| Configuration storage         | ✔️                                                          |
| Message storage               | ❌ (messages are stored on Kafka brokers)                    |
| Required in modern KRaft mode | ❌                                                           |


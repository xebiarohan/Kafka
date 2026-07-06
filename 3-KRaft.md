## What is KRaft?

**KRaft (Kafka Raft)** is Kafka's built-in consensus protocol and metadata management system that **replaces ZooKeeper**.

KRaft stands for **Kafka Raft Metadata Mode**.

Before KRaft:

```text
                +-------------+
                | ZooKeeper   |
                +------+------+
                       |
       --------------------------------
       |              |               |
   Broker 1      Broker 2       Broker 3
```

With KRaft:

```text
      -----------------------------------------
      |              |                |
+-----------+  +-----------+  +-----------+
| Broker 1  |  | Broker 2  |  | Broker 3  |
| Controller|  | Broker    |  | Broker    |
+-----------+  +-----------+  +-----------+
```

Kafka itself manages cluster metadata, so ZooKeeper is no longer required.

---

# Why was KRaft introduced?

ZooKeeper worked well, but maintaining two distributed systems (Kafka + ZooKeeper) introduced challenges:

* Separate cluster to deploy and monitor
* More network communication
* More operational complexity
* Slower metadata propagation
* Harder upgrades
* Scalability limitations for very large clusters

KRaft removes these issues by integrating metadata management into Kafka.

---

# What does KRaft store?

Like ZooKeeper, KRaft stores **metadata**, not messages.

Examples of metadata include:

* Brokers in the cluster
* Topics
* Partitions
* Replication factor
* Leader assignments
* Topic configurations
* ACLs
* Quotas
* Consumer group metadata (depending on Kafka version)

Messages continue to be stored in Kafka topic partitions on brokers.

---

# The Metadata Log

Instead of storing metadata in ZooKeeper, KRaft stores it in a special replicated **metadata log**.

Think of it like this:

```text
Application Data
----------------

Orders Topic

Partition 0
Partition 1
Partition 2

↓

Stored in topic partitions


Metadata
--------

Broker registrations
Topic creation
Leader changes
ACLs
Configurations

↓

Stored in Metadata Log
```

Every metadata change is recorded as an append-only event.

For example:

```text
Create Topic Orders

↓

Metadata Log

Entry #1
```

```text
Create Topic Payments

↓

Metadata Log

Entry #2
```

```text
Broker 5 Joined

↓

Metadata Log

Entry #3
```

This provides a durable history of metadata changes.

---

# KRaft Controllers

In KRaft, a set of nodes forms the **controller quorum**.

Example:

```text
5 Controllers

Controller 1

Controller 2

Controller 3

Controller 4

Controller 5
```

These controllers use the **Raft consensus algorithm**.

Only **one controller is the active leader**.

The others are followers.

```text
Leader Controller

↓

Controller 2

Followers

Controller 1
Controller 3
Controller 4
Controller 5
```

The leader processes metadata changes.

---

# What is Raft?

Raft is a consensus algorithm.

Its purpose is to ensure that multiple nodes agree on the same sequence of changes, even if some nodes fail.

For example:

```text
Create Topic Orders
```

The leader controller receives the request.

Before confirming success:

* It writes the change to its metadata log.
* It replicates the change to follower controllers.
* Once a **majority (quorum)** acknowledges the write, the change is considered committed.

Example with three controllers:

```text
Controller 1

Controller 2

Controller 3
```

Leader:

```text
Controller 1
```

A topic creation request arrives.

Leader writes:

```text
Entry #101
Create Orders Topic
```

Then sends it to the followers.

```text
Controller 2

✓ Stored

Controller 3

✓ Stored
```

Because a majority has acknowledged the entry, it is committed.

---

# Why Majority?

Imagine five controllers:

```text
Controller1

Controller2

Controller3

Controller4

Controller5
```

The majority is **3**.

If Controllers 4 and 5 fail:

```text
Controller1

Controller2

Controller3
```

The cluster can still continue because a majority is available.

If only two controllers remain:

```text
Controller1

Controller2
```

No majority exists, so metadata updates stop until a quorum is restored. This prevents different subsets of controllers from making conflicting decisions.

---

# Broker Registration

When a broker starts:

```text
Broker 7 starts
```

It sends a registration request to the active controller.

The controller appends:

```text
Broker7 Registered
```

to the metadata log.

After the entry is committed, all controllers know Broker 7 is part of the cluster.

---

# Topic Creation

Suppose an administrator creates a topic:

```text
Orders
Partitions = 6
Replication = 3
```

The request goes to the active controller.

The controller appends:

```text
Create Orders Topic
```

to the metadata log.

After the entry is committed:

* All controllers have the updated metadata.
* Brokers receive the new metadata.
* The topic is ready for use.

---

# Leader Election

Suppose:

```text
Orders

Partition 0
```

Replicas:

```text
Broker1

Broker2

Broker3
```

The controller decides:

```text
Leader = Broker2
```

This decision is written to the metadata log.

If Broker2 crashes:

```text
Broker2 Down
```

The controller selects:

```text
Leader = Broker1
```

This new leader assignment is also recorded in the metadata log and propagated to brokers.

---

# Broker Failure

Suppose:

```text
Broker4 crashes
```

The active controller detects the failure (through broker heartbeats and communication).

It writes:

```text
Broker4 Offline
```

to the metadata log.

Then it:

* Elects new partition leaders where necessary.
* Updates replica assignments if required.
* Propagates the updated metadata to brokers.

---

# Metadata Propagation

Every broker maintains a local copy of the committed metadata.

```text
Metadata Log

↓

Broker1

Broker2

Broker3

Broker4
```

When metadata changes, brokers update their local view so they know:

* Current partition leaders
* Available brokers
* Topics and partitions
* Configuration changes

This allows brokers to route client requests correctly.

---

# Combined vs Dedicated Controllers

KRaft supports two deployment modes.

### 1. Combined Mode (Development)

A node acts as both a broker and a controller.

```text
Broker + Controller

Broker + Controller

Broker + Controller
```

This is simple and commonly used for development or small clusters.

### 2. Dedicated Controller Mode (Production)

Some nodes are controllers only.

```text
Controllers

Controller1

Controller2

Controller3


Brokers

Broker1

Broker2

Broker3

Broker4
```

This separation improves scalability and isolates metadata management from client traffic, making it the preferred production setup.

---

# Data Flow in KRaft

### Step 1

The producer connects to a bootstrap broker.

```text
Producer

↓

Broker1
```

### Step 2

Broker1 returns cluster metadata.

```text
Leader

Partition information

Broker list
```

### Step 3

The producer sends data directly to the partition leader.

```text
Producer

↓

Leader Broker
```

### Step 4

The leader writes the message to its partition log.

### Step 5

Followers replicate the data.

Notice that the controller quorum is **not involved in the message path**. Controllers handle metadata changes, while brokers handle client data.

---

# KRaft vs ZooKeeper

| Feature                | ZooKeeper Mode                | KRaft Mode             |
| ---------------------- | ----------------------------- | ---------------------- |
| External dependency    | ZooKeeper required            | No external dependency |
| Metadata storage       | ZooKeeper                     | Kafka metadata log     |
| Consensus algorithm    | ZooKeeper's Zab protocol      | Raft                   |
| Controller election    | Coordinated through ZooKeeper | Raft controller quorum |
| Operational complexity | Higher                        | Lower                  |
| Scalability            | Good                          | Improved               |
| Deployment             | Kafka + ZooKeeper             | Kafka only             |

---

# Complete Flow Example

Imagine a three-broker cluster with a three-node controller quorum.

1. Broker 1, Broker 2, and Broker 3 register with the active controller.
2. An administrator creates the `Orders` topic.
3. The active controller writes the topic creation to the metadata log.
4. The change is replicated to a majority of controllers and committed.
5. Brokers receive the updated metadata and create the required partitions.
6. A producer requests metadata from a bootstrap broker.
7. The broker returns the partition leaders.
8. The producer sends messages directly to the appropriate partition leader.
9. If a broker fails, the active controller records the failure, elects new leaders where needed, commits those changes to the metadata log, and brokers update their local metadata.

---

## Key Takeaways

* **KRaft replaces ZooKeeper** for metadata management.
* It uses the **Raft consensus algorithm** to keep metadata consistent across controllers.
* Metadata is stored in a replicated, append-only **metadata log**.
* Only the **active controller** processes metadata changes; follower controllers replicate them.
* A **majority (quorum)** of controllers must acknowledge a metadata change before it is committed.
* **Controllers manage metadata only**—they do **not** handle producer or consumer data traffic. Messages continue to flow directly between clients and the appropriate Kafka brokers.

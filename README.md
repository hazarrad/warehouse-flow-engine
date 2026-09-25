# Warehouse Flow Engine

A backend warehouse automation engine that dynamically routes totes through a conveyor network, handles congestion and blocked conveyors, simulates tote movement, and publishes warehouse operational events through Kafka.

The project is designed as a simplified **Warehouse Control System (WCS)-style orchestration engine**, focusing on automation and real-time flow control rather than warehouse management.

---

## Business Problem

In an automated warehouse, totes need to move continuously between different areas such as receiving, conveyors, and packing stations.

The system needs to:

* Determine the best route for each tote.
* Consider conveyor capacity and current congestion.
* Avoid blocked conveyor segments.
* Detect movement failures.
* Automatically reroute affected totes.
* Track tote movement and lifecycle.
* Publish operational events for downstream systems.

The goal of this project is to simulate these automation capabilities in a backend service.

---

## Core Capabilities

### 1. Dynamic Tote Routing

The engine calculates the optimal route between two warehouse nodes using **Dijkstra's shortest-path algorithm**.

Routes are based on:

* Conveyor travel time
* Conveyor availability
* Current congestion

Example:

```text
C03
 ├── C04 → C05 → C06 ──┐
 │                      │
 └── C09 → C10 ─────────┤
                        ↓
                       C11
                        ↓
                   PACKING-01
```

---

### 2. Conveyor Runtime State

Each conveyor segment has a runtime state containing:

* Capacity
* Current occupancy
* Status

Supported states:

```text
ACTIVE
BLOCKED
```

The routing engine can therefore avoid conveyor segments that are full or blocked.

---

### 3. Congestion-Aware Routing

The routing engine dynamically adjusts routing cost according to conveyor occupancy.

```text
Routing Cost
    =
Travel Time × Congestion Factor
```

This allows the engine to prefer an alternative route when congestion makes it more efficient.

Physical conveyor travel time remains unchanged; congestion affects the routing decision.

---

### 4. Tote Movement Simulation

The engine simulates tote movement through the conveyor network.

A tote progresses through its calculated route:

```text
C03
 ↓
C09
 ↓
C10
 ↓
C11
 ↓
PACKING-01
```

The tote lifecycle includes:

```text
CREATED
   ↓
MOVING
   ↓
DELIVERED
```

When movement cannot continue:

```text
MOVING
   ↓
WAITING
```

---

### 5. Automatic Rerouting

When a tote cannot enter the next conveyor segment, the engine:

1. Detects the movement failure.
2. Updates the tote to `WAITING`.
3. Calculates a new route from the tote's current location.
4. Resumes movement if an alternative route exists.

Example:

```text
C03 → C09 → C10 → C11
       ✕
       │
       └── blocked

        ↓

C03 → C04 → C05 → C06 → C11
```

This allows the automation flow to recover without manually providing a new route.

---

### 6. Event-Driven Architecture

The engine publishes domain events for important warehouse activities.

Examples:

```text
ToteStartedEvent
ToteWaitingEvent
ToteReroutedEvent
ToteDeliveredEvent
ConveyorBlockedEvent
ConveyorUnblockedEvent
```

Events are published through an abstraction:

```text
Business Logic
      ↓
EventPublisher
      ↓
Kafka
      ↓
warehouse-events
```

Kafka decouples operational events from potential downstream consumers such as monitoring, analytics, or future warehouse services.

---

## Architecture

The project follows a **modular monolith** architecture.

```text
                    ┌─────────────────────┐
                    │    REST API         │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │    Tote Service     │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
       Routing Service   Conveyor State   Recovery Service
              │                │                │
              └────────────────┼────────────────┘
                               ▼
                       Warehouse Graph
                               │
                               ▼
                        Domain Events
                               │
                               ▼
                        EventPublisher
                               │
                               ▼
                             Kafka
                               │
                               ▼
                       warehouse-events
```

The modular monolith keeps the core business logic together while maintaining clear domain boundaries.

Kafka provides an event-driven integration point without introducing unnecessary microservices.

---

## Warehouse Topology

The warehouse topology is configured externally using `application.yml`.

```text
ENTRY
  ↓
 C01
  ↓
 C02
  ↓
 C03
 ├──→ C04 → C05 → C06 ──┐
 │                       ↓
 └──→ C09 → C10 ─────→ C11
                         ↓
                    PACKING-01
```

Example configuration:

```yaml
warehouse:
  nodes:
    - id: ENTRY
      type: ENTRY

    - id: C01
      type: CONVEYOR

    - id: C03
      type: CONVEYOR

    - id: PACKING-01
      type: PACKING

  edges:
    - from: ENTRY
      to: C01
      travel-time-seconds: 2
      capacity: 3

    - from: C03
      to: C09
      travel-time-seconds: 2
      capacity: 3
```

This allows the warehouse topology to be changed without modifying the routing algorithm.

---

## Example API

### Calculate a Route

```http
POST /api/v1/totes/TOTE-001/route
Content-Type: application/json
```

Request:

```json
{
  "currentNode": "C03",
  "destination": "PACKING-01",
  "priority": "NORMAL"
}
```

Response:

```json
{
  "toteId": "TOTE-001",
  "route": [
    "C03",
    "C09",
    "C10",
    "C11",
    "PACKING-01"
  ],
  "estimatedTravelTimeSeconds": 8
}
```

### Start a Tote

```http
POST /api/v1/totes/TOTE-001/start
```

### Get Tote State

```http
GET /api/v1/totes/TOTE-001
```

### Conveyor State

```http
GET /api/v1/conveyors/C03/C09
```

Runtime operations include:

```text
POST /enter
POST /leave
POST /block
POST /unblock
```

---

## Event Example

Events are published to:

```text
warehouse-events
```

Each Kafka message is wrapped in an event envelope:

```json
{
  "eventId": "8d9c...",
  "eventType": "ToteStartedEvent",
  "timestamp": "2026-09-25T20:15:30Z",
  "payload": {
    "toteId": "TOTE-001",
    "currentNode": "C03",
    "destination": "PACKING-01"
  }
}
```

The events can be inspected locally using **Kafbat Kafka UI**.

---

## Technology Stack

| Technology        | Purpose                       |
| ----------------- | ----------------------------- |
| Java 21           | Application development       |
| Spring Boot       | Backend framework             |
| Spring Web        | REST APIs                     |
| Spring Validation | Request validation            |
| Spring Kafka      | Kafka integration             |
| Apache Kafka      | Event streaming               |
| Docker            | Local infrastructure          |
| Kafbat Kafka UI   | Kafka monitoring              |
| Maven             | Build & dependency management |
| JUnit 5           | Testing                       |
| Mockito           | Unit testing                  |

---

## Project Structure

```text
src/main/java
└── com.github.hazarrad.warehouseflow
    ├── config
    │   └── WarehouseGraphConfig
    │
    ├── controller
    │   ├── ToteController
    │   └── ConveyorController
    │
    ├── domain
    │   ├── Edge
    │   ├── Node
    │   ├── Tote
    │   ├── ToteStatus
    │   ├── TotePriority
    │   └── ConveyorStatus
    │
    ├── event
    │   ├── WarehouseEvent
    │   ├── ToteStartedEvent
    │   ├── ToteWaitingEvent
    │   ├── ToteReroutedEvent
    │   ├── ToteDeliveredEvent
    │   ├── ConveyorBlockedEvent
    │   └── ConveyorUnblockedEvent
    │
    ├── kafka
    │   └── KafkaEventPublisher
    │
    ├── routing
    │   └── RoutingService
    │
    ├── recovery
    │   └── RecoveryService
    │
    ├── conveyor
    │   ├── ConveyorState
    │   └── ConveyorStateService
    │
    └── tote
        ├── ToteService
        └── ToteMovementSimulator
```

---

## Running Locally

### 1. Start Kafka

Start the local Kafka infrastructure using Docker Compose.

```bash
docker compose up -d
```

### 2. Start the Spring Boot application

```bash
./mvnw spring-boot:run
```

Or:

```bash
mvn spring-boot:run
```

### 3. Open Kafbat

Open the local Kafbat UI configured in the Docker Compose setup.

The Kafka topic used by the application is:

```text
warehouse-events
```

### 4. Test the API

Example:

```bash
curl -X POST http://localhost:8080/api/v1/totes/TOTE-001/start \
  -H "Content-Type: application/json" \
  -d '{
    "currentNode": "C03",
    "destination": "PACKING-01",
    "priority": "NORMAL"
  }'
```

---

## Testing

The project includes unit tests covering the main automation scenarios.

Examples include:

* Shortest route calculation
* Unknown nodes
* Conveyor capacity
* Conveyor occupancy
* Blocked conveyors
* Congestion-aware routing
* Tote movement
* Tote delivery
* Movement failures
* Automatic rerouting
* Domain event publishing

Run tests with:

```bash
./mvnw test
```

---

## Design Decisions

### Why a Modular Monolith?

The project keeps the core automation logic in a single application while maintaining clear module boundaries.

This avoids introducing microservices before there is a real business or operational need for service separation.

### Why Kafka?

Kafka provides asynchronous event distribution for warehouse operational events.

The core automation flow does not depend directly on Kafka:

```text
Business Logic
      ↓
EventPublisher
      ↓
KafkaEventPublisher
      ↓
Kafka
```

This keeps infrastructure concerns separated from the core domain logic.

### Why Configuration-Based Topology?

Warehouse layouts can change.

Keeping nodes, edges, travel times, and capacities in configuration allows the routing engine to operate on different warehouse layouts without changing its algorithm.

---

## Project Roadmap

The implementation was developed incrementally:

```text
Phase 1 — Warehouse Topology
Phase 2 — Shortest-Path Routing
Phase 3 — Conveyor Runtime State
Phase 4 — Congestion-Aware Routing
Phase 5 — Tote Movement Simulation
Phase 6 — Automatic Rerouting
Phase 7 — Event-Driven Architecture with Kafka
```

**Status: Complete**

---

## What This Project Demonstrates

This project demonstrates practical backend engineering concepts including:

* Graph-based routing
* Dijkstra's algorithm
* State management
* Capacity and congestion handling
* Failure recovery
* Asynchronous processing
* Domain events
* Event-driven architecture
* Kafka integration
* Configuration-driven design
* REST API design
* Unit testing
* Modular monolith architecture

The main focus is not simply moving totes through a graph, but designing a backend system that can **make routing decisions, react to operational constraints, recover from failures, and publish events as the warehouse flow evolves.**

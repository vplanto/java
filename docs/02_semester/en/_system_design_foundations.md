# System Design Foundations: Why code is only a detail. Mindset, NFRs, and artifacts (Extended)

> **Other language:** [Українська версія](../_system_design_foundations.md)

**Audience:** Future System Architects / Tech Leads  
**Goal:** Understand the difference between writing code and designing systems. Learn to see the “invisible” runtime requirements and reason in trade-offs.

---

### 1. Rapid activation (warm-up)

**Question 1:** What is the fundamental difference between a Senior Developer’s job and a Solution Architect’s?

<details>
<summary>Answer</summary>

* **Senior Developer:** Focus on **local optimization** (Clean Code, SOLID, algorithmic complexity inside a function).
* *KPI:* Code Coverage, Cyclomatic Complexity.
* *Planning horizon:* Sprint – Month.

* **Architect:** Focus on **global optimization** (TCO — Total Cost of Ownership, MTTD/MTTR, CAP theorem). Decisions are often suboptimal for code, but optimal for the business.
* *KPI:* Scalability (RPS), Availability (SLA/SLO), Cost Efficiency.
* *Planning horizon:* 1 year – 5 years.
</details>

**Question 2:** You have perfect code with 100% test coverage. But in Production the system crashes. Why?

<details>
<summary>Answer</summary>

The problem is **runtime constraints**. Unit tests live in a vacuum.  
Production has:
* **Noisy Neighbors:** Nearby K8s pods ate CPU (throttling).
* **Network Saturation:** TCP retransmits due to a bad link.
* **Resource Limits:** OOMKilled due to unaccounted JVM/Go runtime overhead.
* **Connection Exhaustion:** Ran out of free file descriptors or DB pool ports.
</details>

**Question 3:** If AI can generate code in seconds, why do we need an architect?

<details>
<summary>Answer</summary>

AI generates **implementation**, not **strategy**.  
An architect is a **Director of Code** who:

1. Validates design against NFRs (Non-Functional Requirements).
2. Manages technical debt.
3. Makes decisions under uncertainty (where AI hallucinates).
</details>

---

## 2. Vibe Coding philosophy: engineer as Director

The world changed. We no longer type code by hand—we direct its generation.  
**Vibe Coding** is not “relaxed coding”; it’s a role shift from **Builder** to **Architect/Director**.

### Engineer exoskeleton

* **Old Way (Typing):** You think about syntax, commas, and brackets. Speed is limited by your fingers.
* **New Way (Directing):** You think in **intent** and **constraints**.

#### Example dialogue (prompt engineering for architects)

* *You:* “I need an order processing service that can handle 10k RPS.”
* *AI:* Generates a simple REST controller writing to a DB.
* *You (Validation & Hardening):* “You used a synchronous DB call. At 10k RPS that creates a connection storm.
1. Redesign to an async model via Kafka.
2. Add an **Idempotency Key** to prevent duplicates.
3. Implement **Graceful Shutdown** so messages aren’t lost during redeploys.”

> **Golden Vibe Coding rule:** You don’t accept AI code unless you can explain how it behaves under load. AI is your “draftsman”, but you sign the blueprint. You’re responsible for **corner cases** and **failure modes**.

---

## 3. Code vs execution environment: where does your system live?

A common beginner mistake is assuming requirements come only from the business (features).  
A professional architect knows: **50% of requirements are dictated by the runtime environment.**

### The “hidden” requirements stack (runtime constraints)

1. **Infrastructure leakage**
In modern cloud-native systems, abstractions leak.
* *Example:* `Retry` logic.
* *Bad Practice:* Hardcoding retry policies in the app (via libraries like Resilience4j or HTTP client configs), causing retry config drift across services.
* *Good Practice:* Delegate to a sidecar (Envoy/Istio) with **exponential backoff + jitter** to prevent “thundering herd” amplification (everyone retries at once and finishes the service off).

2. **Maintenance reality**
Systems don’t run forever without intervention.
* *Scenario:* Your Load Balancer certificate expired, or you need a DB schema upgrade.
* *Pattern:* **Zero Downtime Deployment**.
* Database: “Expand and Contract” (add column -> ship code -> migrate data -> delete old column).
* Service: Readiness/Liveness probes for safe traffic switching.

3. **Deployment safety**
You can’t just “push” new code.
* *Requirement:* Architecture must support **Observability First**.
* Code must export metrics (Prometheus) and traces (OpenTelemetry) *before* it reaches prod. If you can’t see `latency_bucket`, you’re blind.

### 🛑 Fallacies of distributed computing — deep dive

New architects often design for an ideal world. The real world is brutal.

1. **The network is reliable.**
* *Reality:* TCP timeouts, DNS failures, BGP rerouting.
* *Solution:* Idempotency (safe retries), Dead Letter Queues (DLQ).

2. **Latency is zero.**
* *Reality:* Kyiv–New York is ~120ms (physics of light). N+1 DB calls across the ocean kill UX.
* *Solution:* Data locality, caching (CDN, Redis), batching.

3. **Bandwidth is infinite.**
* *Reality:* A 1Gbps link is easy to saturate with uncompressed JSON or logs.
* *Solution:* Protobuf/gRPC (binary), compression (Gzip/Snappy), pagination.

4. **The network is secure.**
* *Reality:* Sniffing, MITM.
* *Solution:* mTLS (Mutual TLS), Zero Trust Architecture.

5. **Topology doesn’t change.**
* *Reality:* Pods in K8s are ephemeral (pets vs cattle). IPs change constantly.
* *Solution:* Service discovery (Consul, K8s DNS), client-side load balancing.

6. **Transport cost is zero.**
* *Reality:* Serialization/deserialization (marshalling) costs CPU.
* *Solution:* Efficient schemas, CPU profiling (flamegraphs).

---

## 4. Design methodologies: how do you start a project?

With a blank page, panic is normal. How do you turn an idea into architecture? There are two fundamental approaches.

### Top-down — domain-driven focus

* **Philosophy:** “From user need to hardware.”
* **Process:**
1. **User Needs & Ubiquitous Language:** Work with the business; identify bounded contexts.
2. **API Design (Contract First):** OpenAPI (Swagger). Agree on the contract, then code.
3. **High-level components:** C4 Model (Context -> Container -> Component).
4. **Database & Infra:** Choose CAP trade-offs (Consistency vs Availability).

* **When to use:** Greenfield, complex business domains where logic matters more than raw performance.
* **Vibe Coding Tip:** “Generate a PlantUML sequence diagram for the taxi ordering flow.”

### 🧱 Bottom-up — data-driven focus

* **Philosophy:** “From existing capabilities to new features.”
* **Process:**
1. **Data & Capability:** Analyze schema, bus throughput, existing logs.
2. **Service Layer (Composition):** “Strangler Fig” pattern (gradual extraction from a monolith).
3. **API:** Exposure layer (GraphQL/BFF).
4. **UI:** Present aggregated data.

* **When to use:** Legacy modernization, refactoring, hard hardware constraints (embedded).

---

## 5. The architect’s map: North, South, East, West

In complex distributed systems, it’s easy to get lost. Architects use cardinal directions to standardize data flows and protocol choices.

### Northbound: front door

This is your public facade. The environment is hostile (Internet).

* **Clients:** SPA, mobile, IoT devices.
* **Protocols:** HTTP/2 (REST), GraphQL (UI flexibility), WebSockets.
* **Components:**
* **API Gateway (Kong/Nginx):** Routing, SSL termination.
* **BFF (Backend for Frontend):** Data aggregation for a specific UI.
* **WAF:** Protection against SQL Injection, XSS.

* **Architectural focus:** Security, rate limiting (Token Bucket), caching strategies (ETag, Cache-Control).

### Southbound: foundation and state

These are stateful systems. This is where “state” lives.

* **Components:**
* **RDBMS (Postgres):** ACID transactions.
* **NoSQL (Cassandra/DynamoDB):** High write throughput, eventual consistency.
* **Message Brokers (Kafka/RabbitMQ):** Async.

* **Architectural focus:**
* **CAP Theorem:** What do we sacrifice during partitioning? (Consistency or Availability.)
* **Connection Pooling:** PgBouncer (don’t open a socket per request).
* **Data Partitioning/Sharding:** Horizontal scaling for data.

### East-West: internal communication (data center)

Traffic between microservices inside a VPC/cluster.

* **Requirements:** High throughput, low latency.
* **Protocols:** gRPC (Protobuf) — faster than JSON, Thrift.
* **Components:** Service Mesh (Istio/Linkerd).
* **Architectural focus:**
* **Resiliency Patterns:** Circuit Breaker (Hystrix/Resilience4j) — “don’t knock on a dead service”.
* **Bulkhead:** Isolate thread pools (so service A’s failure doesn’t take down service B).
* **Distributed Tracing:** Propagate `TraceID` through every call.

---

## 6. Teamwork & engineering friction: how not to kill each other

Architecture is a **way to resolve friction** in a team and to capture trade-offs.

### Architect’s artifacts (what you’re paid for)

> **Golden rule:** If a decision isn’t documented, it doesn’t exist.

#### 1. Architecture Blueprint (high-level design)

The project’s “constitution”.

* **Tech Stack:** Java/Go, Postgres/Mongo.
* **NFR Values:** RTO/RPO (Recovery Time/Point Objective) — how much data can we lose in an incident?

#### 2. ADR (Architecture Decision Records) — critical

It’s the flight surgeon’s logbook.

* **Format:**
* **Context:** We need to choose a message broker.
* **Options:** Kafka vs RabbitMQ.
* **Decision:** Choose Kafka.
* **Consequences (Trade-offs):** High throughput, BUT more infrastructure complexity and less flexible routing than RabbitMQ.

* *Why it matters:* In a year, nobody will remember why Kafka was chosen. ADR protects against the “new manager effect.”

#### 3. Impact Analysis document

* **Matrix of Dependencies:** “Service A depends on API v1 of Service B”.
* **Migration Strategy:** Blue-Green, Canary, or Big Bang (only if there’s no alternative).

#### 4. Rollout checklist (runbook)

* Pre-deployment: Schema migration (`alembic upgrade head`).
* Verification: `curl /health` returns 200.
* Rollback: `helm rollback`.

---

## 7. Visualization: how do we “sell” the solution?

An architect must speak to different audiences. C4 Model (Context, Containers, Components, Code) is an industry standard.

### Level A: logical design (conceptual) — “what”

* **Audience:** Business, PM.
* **Goal:** Show **value streams**.
* **Example:** Customer -> Order -> Payment -> Delivery. No servers—only business entities.

### Level B: physical design (infrastructure) — “where”

* **Audience:** DevOps, SRE.
* **Goal:** Deployment topology.
* **Example:**
* “Orders” -> K8s Deployment (3 replicas), Requests: 200m CPU.
* “Payments” -> External API Stripe.
* “Connectivity” -> Ingress Controller, TLS v1.3.

---

## 8. The cost of an architectural mistake: post-mortem analysis

An architect is the person who makes the **most expensive decisions** before the first line of code is written. A design-stage mistake costs 100x more than a bug in code.

#### 🔴 Case 1: the “Ticketmaster” effect (distributed locking fail)

* **Problem:** While selling tickets, thousands of users tried to buy *the same* seat at the same time.
* **Root cause:** Using optimistic locking (DB-level) instead of a distributed cache (Redis/Lua scripts) to reserve slots. The DB “went down” due to `ROLLBACK` volume.
* **Lesson:** For high-contention resources (tickets, inventory), you need a queue or in-memory serialization—not direct DB writes.

#### 🔴 Case 2: Mars Climate Orbiter ($327M) — integration hell

* **Essence:** Metric vs Imperial units.
* **Lesson:** Missing **contract testing** (Pact) and strict typing at the API schema level. Interfaces must be strongly typed (Strongly Typed IDL).

#### 🔴 Case 3: cascading failures (domino effect)

* **Scenario:** The recommendations service failed. The store homepage stopped loading too.
* **Cause:** No **timeouts** and strong coupling. The frontend waited forever.
* **Lesson:** **Graceful degradation**. If recommendations are down, show “Top Sellers” or an empty block, but let the user buy.

---

### Discussion question

*Situation: You’re building a system for a Tier 1 telecom operator. You need to add analytics.*  
**Question:** Which direction (N/S/E/W) does integration with external Google Analytics belong to? What about an internal Hadoop Data Warehouse?

<details>
<summary>Answer</summary>

* **Google Analytics:** Client-side integration; from backend perspective it’s invisible. For server-side tracking, it’s **Southbound** (an external sink we write to without waiting for a response).
* **Internal Data Warehouse:** **Southbound** (if we write to HDFS) or **East-West** (if ETL pulls from our Kafka). Key point: ETL must not degrade the operational database (isolation of workloads).

</details>

---

### 9. Final check questions (checklist)

1. **On NFRs:** Why is “fast” a bad requirement? How should an architect specify performance requirements?
<details>
<summary>Answer</summary>

“Fast” can’t be measured. An architect says: “P99 Latency < 200ms at 10k RPS”. That means 99% of requests are faster than 200ms even under load.
</details>

2. **On artifacts:** You’re changing the billing database schema. Which document do you prepare first?
<details>
<summary>Answer</summary>

**Impact Analysis** (who is affected) and **ADR** (why we do it and how we migrate).
</details>

3. **On directions:** Where does the Load Balancer belong on an architecture diagram?
<details>
<summary>Answer</summary>

**Northbound**. It’s the system’s shield.
</details>

4. **On fallacies:** Your team says: “We won’t handle network timeouts because our datacenter is reliable.” What mistake are they making?

<details>
<summary>Answer</summary>
The “Network is reliable” trap. You need **Retry (with jitter)** and a **Circuit Breaker**.
</details>



# Practice: System Design Workshop. “URL Shortener” Practice

> **Other language:** [Українська версія](../_system_design_workshop.md)

**Audience:** 2nd year (Junior Engineers)  
**Format:** Live Design Session  
**Goal:** Go from the abstract idea “build me something like bit.ly” to a concrete database schema and algorithm choices, using AI as a partner.

---

## ⚡ 1. Rapid activation: What are we building?

Everyone knows services like `bit.ly`. You paste a long link and get a short one.  
It looks like a 15-minute task for a 1st-year student.

**Million-dollar question:** If it’s so simple, why do engineers fail this task in Google interviews?

<details>
<summary>Answer</summary>

Because writing code that works for **one** user is easy.  
Building a system that works for **30 million** users, doesn’t fall over when the network misbehaves, and guarantees link uniqueness—that’s engineering.

</details>

### Mental model: the “elevator doors” case (response time)

Before talking about the web, let’s define what “fast” means.

Imagine you approach an elevator, the doors start closing, but the sensor detects you and must reopen them.  
How much time does the system have?

* **Sensor (Detect):** 5–15 ms.
* **Controller (Software Logic):** 16–48 μs (microseconds!).
* **Door mechanics (Motor Drive):** 300–500 ms.

> **Answer:** Total system reaction time (from sensor trigger to door movement) is **305 to 515 ms**.

**What this means for us:** A human doesn’t notice latency < 100 ms. But for a computer (CPU L1 cache = 1 ns) that’s an eternity.

> **Conclusion:** Your 200 ms link generation is a compromise between “instant for a human” and “long enough for a server.”

### Why is 100 ms the boundary? (Biology and esports)

We often set an NFR (Non-Functional Requirements) of Latency < 100 ms. Why that number? It’s not random—it’s biological.

1. **Biology: saccadic masking**
The brain doesn’t perceive reality in real time. It needs time to process input (brain input lag).
* **Blink duration:** one blink lasts **100–150 ms**.
* **Saccadic masking:** we don’t see darkness when we blink. The brain **disables visual input** for ~100 ms to stabilize the picture and “fills in” the missing frame.
* **Engineering conclusion:** UI latency up to 100 ms feels “instant” because it fits into our natural biological “blind window.” We’re used to the world disappearing for 100 ms.

2. **Esports: the fight for milliseconds**
In competitive games (CS:GO, Valorant) the requirements are stricter.
* **Average person:** visual reaction time (see -> click) ≈ **215–250 ms**.
* **Top esports players (s1mple, Faker):** peak reaction time **130–150 ms**. *This is a physical limit:* the signal can’t travel through nerves faster.
* **Why is 50 ms ping “a lot”?** Total lag = (Reaction Time) + (Mouse Input Lag) + (Monitor Refresh Rate) + (Network Ping). If a player’s reaction is 140 ms and ping is 60 ms, total latency is 200 ms—meaning the player will see the enemy when the enemy has already shot.

> **Architectural conclusion:** If your backend “hangs” for 200 ms (for example due to a long Java GC pause), you’re slower than a human nerve impulse.
> * For **Web UI**: < 100 ms (normal).
> * For **Game Server**: < 30 ms (otherwise it feels “laggy”).
> * For **HFT (Trading)**: < 1 ms (robots compete; human limits don’t apply).

---

## Stage 1. Requirements Engineering (interrogating the customer)

*Simulation: you are the Architect. I’m the customer (Product Owner).*
Don’t start drawing boxes. Start with numbers and business goals.

### 1.0 Feasibility Study

Before coding, we must understand whether it’s worth doing.

* **Feasibility Study:** Do we have budget and time?
* **Alternatives Analysis:** Compare options:
* *Option A:* Build from scratch.
* *Option B:* Buy SaaS (ready API like Bitly Enterprise).
* *Option C:* Serverless (AWS Lambda) vs Dedicated Clusters.
* **Task & Skill Assessment:** Does the team know Go/Java well enough?

* **Artifact Yield:** **Scope of Work (SOW)** document.

### 1.1 Functional requirements

This part is simple:

1. **Shorten:** The system accepts a long URL and returns a unique short key (e.g., 7 characters).
2. **Redirect:** When visiting the short link, the user is redirected to the original resource.

**Modeling methodology:**  
Sources recommend not just writing text, but visualizing processes:

* **DFD (Data Flow Diagram):** How data flows through the system (User -> API -> DB).
* **ERD (Entity-Relationship Diagram):** Conceptual data model.
* **Functional Modeling:** A description of organizational operations for documenting business processes.

> **Artifact Yield:** **System Requirements Document** (includes not only functionality, but also an analysis of existing hardware/software).

### 1.2 Non-functional requirements (NFR & constraints) — the most important part

We use **Vibe Coding** for back-of-the-envelope calculations.

> **AI Prompt (Drafting):**
> "Ти — System Architect. Допоможи мені оцінити навантаження (Capacity Planning) для URL Shortener.
> Вхідні дані: 30 мільйонів користувачів на місяць. Зберігаємо посилання 5 років.
> Порахуй: 1) Requests Per Second (RPS), 2) Загальний обсяг сховища."

**Validation (engineering reality check):**

* **Traffic:** 30M users/month. That implies steady request volume. Reads (Redirect) are always higher than writes (Shorten). Assume a 10:1 ratio.
* **Storage:** One record: ~2 KB (URL up to 2048 characters + metadata).
* Records over 5 years: ~1.8B.
* **Total storage:** ~3.6 TB.

**Architectural conclusion:**

1. 3.6 TB won’t fit into a single server’s memory -> we need a **disk-backed database**.
2. Searching 3.6 TB on every click is slow -> we need a **cache (Redis)**.
3. 30M users require high availability -> **Availability 99.9%** (max 8 hours 46 minutes of downtime per year).

---

## Stage 2. Deep dive: shortener algorithm and analytics

How do we turn a long URL into a string like `Abz21TY`?

### Option A: hashing (MD5/SHA-256)

* *Idea:* Take MD5 of the long URL and truncate to 7 characters.
* *Critique:* MD5 is 128 bits; truncation causes **collisions** (two different URLs can produce the same code).
* *Decision:* Rejected.

### Option B (naive): Base62 + Random

* *Idea:* Generate a random 7-character string from [a-zA-Z0-9].
* *Problem (Collision Hell):*
1. Generate `Abcde12`.
2. Query the DB to check if it’s taken.
3. If taken -> repeat.

* *Result:* Works fast at first. When the DB is 70% full, every second attempt collides. The more links you have, the more collisions you hit. Latency grows exponentially (retry storm). This is an architectural mistake.

### Option C (winner): Base62 + Counter (pre-generated)

* *Idea:* Each URL gets a unique number (ID) in the database: 1, 2, 3... 100500.
* *Magic:* Convert that number from decimal (0–9) into **base-62** (0–9, a–z, A–Z).
* *Why 62?* `10 цифр + 26 малих літер + 26 великих літер = 62`.
* *Capacity:* 7 Base62 characters give (trillions) of combinations—enough for 100 years.

> **Vibe Coding Task:**
> Ask AI to write a `base62_encode(long id)` function in Java. Verify it handles negative numbers.

### Event processing (Event Decisioning Algorithm)

We don’t just redirect; we collect analytics for marketers. How do we do that without slowing down the user?
We use an **8-step decision-making algorithm (Event Processing Decisioning)**:

1. **Receive:** Receive the redirect request (System Event).
2. **Validate & Enrich:** Validate the link exists, add a timestamp.
3. **Smart Selection:** Logic like **A/B Split** or **Smart Selected** (ML selects which site version to route the user to based on segment).
4. **Route:** Immediately return a 301 Redirect (to keep Latency < 100ms).
5. **Async Process (Kafka):** Then the event continues in the background.
* **Filter:** Drop bots via rules (Event Rules).
* **Analyze:** Determine geo and user-agent patterns.
* **Correlate:** Link to User ID and campaign.
* **Time Window:** Aggregate clicks per 1 minute.
* **Generate Business Event:** Create a clean “Click” event for reporting.

---

## Stage 3. High-level design (the architect’s compass)

We draw the system map using the cardinal directions.

### Architectural principles

Before drawing, remember the architect’s mantras:

1. **KEEP IT SIMPLE:** Don’t build a spaceship for a bicycle.
2. **YAGNI (You Aren't Gonna Need It):** Don’t add AI if you just need an `if`.
3. **DRY (Don't Repeat Yourself):** Extract shared logic into libraries.

**Vibe Coding Tip (RAG):** Use AI with **RAG (Retrieval-Augmented Generation)**. Load your project docs and corporate security standards so it generates solutions compatible with your infrastructure—not generic internet advice.

### ⬆️ Northbound: Ingress

* **Clients:** Browsers, mobile apps.
* **Load Balancer (LB):** (e.g., HAProxy/Nginx). Takes the first удар. Distributes traffic across application instances.

### ⏺️ Center: application layer

* **App Service (Java/Spring Boot):** The “brain” of the system. It’s stateless, so we can run 50 instances in parallel.
* API: `shortenURL(longUrl)`, `decodeURL(shortUrl)`.

### ⬇️ Southbound: data (state)

* **Database (PostgreSQL):** Stores the “golden copy” of data (mapping: ID -> LongURL).
* **Cache (Redis):** Stores “hot” links for instant access (< 10ms). **In-memory processing** is key for high performance.

### ⬅️ East-West: supporting services

* **ID Generator (Zookeeper):** A service that hands out unique number ranges so servers don’t create duplicates.

### Architect’s formulas

When designing, keep these simple equivalences in mind:

* **Scalability = Partitioning (Sharding)**. If you want to grow, learn to split data into chunks (cut up the database).
* **Reliability = Replication**. If you don’t want to fall over, make copies (Master-Slave).

---

## Stage 4. Data design (schema)

We chose a relational database (PostgreSQL) because we need reliability and transactionality.

**SQL vs NoSQL (the trade-off):**

* **SQL (Relational):** Best for structured data (“noun tables” like User, URL). Guarantees ACID.
* **NoSQL (Document/Key-Value):** Schema flexibility. Redis (Key-Value) is perfect for cache, but as a primary DB for relationships it can be hard.

### `urls` table (logical data model)

* `id`: `integer IDENTITY PK` (auto-increment or Snowflake ID).
* `long_url`: `varchar(2048)` (original link).
* `short_url`: `varchar(7)` (Base62 result, indexed for lookup).
* `created_at`: `timestamp` (for deleting old links).

> **Trick question:** Why is an `Auto-increment` ID bad for the business?
> *Answer:* Competitors can iterate `bit.ly/1`, `bit.ly/2` and learn how many links you have. In reality, people use randomized IDs (Snowflake) or add “salt”.

### Spec detail (yields)

We move from “boxes” to specifics. The final document should include:

1. **Data Dictionary:** Define every attribute, its type, constraints, and source.
2. **Detailed Inputs & Outputs:** Describe all input forms, reports, and API contracts.
 
> **Artifact Yield:** **System Design Specification** (includes the relational model and the data dictionary).

---

## Stage 5. Teamwork & friction

The architecture is done. Now Dev Lead and process work begins.

**What do you do as the Architect and the Lead?**

1. **Conflict:** The dev team wants Zookeeper for perfect ID generation. Ops blocks it due to operational complexity.
2. **Artifact (ADR):** You write an *Architecture Decision Record*.
* *Decision:* Replace Zookeeper with a **Postgres Sequence** with step 1000.
* *How it works:* Server A takes range 1–1000. Server B takes 1001–2000. No Zookeeper.
* *Trade-off:* If Server A dies, we lose unused IDs from its range. That’s acceptable (we have 3.5 trillion).
3. **LOE Assessment (Estimation):** As Dev Lead, estimate how long implementation takes (high-level estimation).
4. **Traceability Matrix:** Verify all Stage 1 requirements are covered by the design. Did we forget Multi-language UI? (What is not covered?).
5. **Task Breakdown (bug-tracking system):** Break architecture blocks into granular tasks. Not “build the DB,” but a WBS (Work Breakdown Structure).
6. **Branching Model Development:** Define branching strategy (merges, rebases) and integration cadence so the team doesn’t block each other.
7. **Statistic Generation:** Key nuance: every new DB entity should ship with generated statistics (Oracle/Postgres statistics). If not, the query optimizer may pick a bad plan and performance will drop immediately after release.
8. **Technical Debt Tracking:** If we take a “quick” shortcut (e.g., hardcoding instead of config), create a JIRA ticket tagged “Technical Debt” so it doesn’t get forgotten.

---

## Stage 6. Runtime reality & operations (surviving in Production)

We drew the diagram—but how does it behave in reality?

### 1. Observability readiness & profiling

We don’t deploy blind.

* **Metrics:** Traffic, latency, errors... and **saturation** (how packed CPU/RAM are).
* **Data Quality Validation:** How do we verify post-release that data is written correctly?
* **Continuous Profiling:** Add continuous profiling (**Heatmap -> Flamegraph**) to see where (Java/Node.js) you lose CPU (e.g., via `async-profiler` or `Clinic.js`). This finds bottlenecks that ordinary charts don’t reveal.
* **Memory Patterns:** Post-test checklist: is Young GC time stable? Is there steady memory growth (potential leak)?

### 2. Failover scenarios

* **Load Balancer Failover:** What happens if one LB node dies? Will DNS switch traffic?
* **Certificate Expiry:** What happens when the SSL certificate expires? Do we have auto-renewal?
* **Certificate Revocation:** Test the scenario where a certificate is compromised and revoked (CRL/OCSP).
* **Disaster Recovery (DR) Validation:** Full verification of restoring from backups in another region.

### 3. System limits & kernel tuning

* **Bottleneck:** You estimated RPS—but did you validate OS limits?
* `ulimit -n`: How many open sockets can Linux hold? If it’s 1024 (default), you die at user 1025.
* **`net.core.somaxconn`:** Backlog queue size for new TCP connections. If too small, clients get Connection Refused under peak load even if CPU is idle.

---

## Stage 7. Financial impact and FinOps

We designed a working system. But how much does it cost?
**Rule:** An architect is responsible not only for uptime, but also for not bankrupting the company (“Cloud Bill Shock”).

### 1. The 3.6 TB problem (storage tiering)

We estimated 3.6 TB over 5 years.

* **Naive Approach:** Keep everything in PostgreSQL on SSD (AWS EBS gp3).
* *Cost:* ~$0.08/GB * 3600 GB = **$288/mo** (disk only, excluding the instance).
* *Problem:* 90% of links aren’t opened after a month (cold data). Why pay for them as “hot”?

* **Architectural Solution (Tiering):**
* Keep “fresh” links (last 3 months) in Postgres.
* Archive older links to **S3 (Object Storage)** or **S3 Glacier**.
* *S3 cost:* ~$0.023/GB = **$82/mo** (3.5x cheaper).
* *Trade-off:* Clicking a 5-year-old link may take 200 ms (fetch from S3) instead of 20 ms. Acceptable.

### 2. Traffic costs (the hidden killer)

We plan for 30M users.

* **Ingress:** Usually free.
* **Egress:** Paid. Every redirect is an HTTP response.
* **Vibe Coding Task:** “Estimate Data Transfer Out cost for 30M hits at 500 bytes (HTTP headers).”
* *Result:* Not much (~15GB/mo). But if we add preview images, cost spikes immediately.

### 3. Unit economics

As Dev Lead, compute: **How much does creating 1000 links cost us?**  
If we earn $0.50 per 1000 impressions but infra costs $0.60, we’re burning investor money.

### 4. Cloud vs on-prem (FinOps decision)

* **Serverless (Lambda/Cloud Run):** Pay only when there’s traffic. Great early on (pay-as-you-go). But at high scale (high RPS) it becomes more expensive than a rented server.
* **Dedicated (EC2/K8s):** Fixed cost. Efficient with stable 24/7 load.

> **Architectural conclusion:** Start with serverless (save on ops), but design for a move to containers (K8s) once the bill exceeds $500/mo.

---

## ✅ Final architect checklist

Validate the solution against the “holy trinity” of NFRs:

1. **Scalability:** Can we handle growth?
* *Yes:* App servers scale horizontally (add nodes). The database can be sharded by ID.

2. **Performance:** Will it be fast?
* *Yes:* Reads go through Redis (cache). Writes are asynchronous. Expected P90 latency < 200 ms. We use in-memory and TCP tuning.

3. **Availability:** Will the system fail if one server burns down?
* *No:* The Load Balancer routes traffic to healthy servers. Redis and Postgres have replicas (Master-Slave).

---

## 🛠 Homework: Vibe Coding Challenge

**Task:** Using AI (Gemini/ChatGPT), generate an **OpenAPI (Swagger) specification** for this service.

**Constraints:**

1. AI must generate a YAML file.
2. **Reality Check (Wrong Assumptions):** You must find where AI made a mistake assuming the “network is reliable” or “latency is zero”.
3. You must find a common error: AI often forgets HTTP error codes (404 Not Found, 429 Too Many Requests).
4. You must add an `expiration_date` field to the request that AI will skip.

**Submission artifact:** A link to a GitHub Gist with the corrected Swagger file and a comment: *“Which Wrong Assumptions did AI make, and how did I fix them?”*



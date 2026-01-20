# Case Study: Why the CAP Theorem Is (Almost) a Myth in the Cloud

> **Other language:** [Українська версія](../case_study_cap.md)

**Source:** Marc Brooker, Principal Engineer at AWS (builds EBS, Lambda).  
**Topic:** Distributed systems, CAP, PACELC.  
**Level:** Advanced / Architecture mindset.

---

## 1. Context: What do they tell you at university?

In interviews and textbooks, you hear the CAP mantra (Brewer’s Theorem):

> *“In a distributed system, it’s impossible to simultaneously guarantee all three properties: **C**onsistency, **A**vailability, and **P**artition Tolerance. Pick two.”*

Usually, it’s presented as a hard engineering verdict:

* Want perfect data (CP)? Be ready for the system to “go down” (become unavailable) during network failures.
* Want the system to always respond (AP)? Be ready to serve stale data (Eventual Consistency).

**Engineering reality:** This is a **false dichotomy** for 99% of modern web services.

---

## 2. Article breakdown: “Let’s Consign CAP to the Cabinet of Curiosities”

Marc Brooker, someone who builds the AWS cloud, argues: **“CAP is irrelevant for most engineers building systems in the cloud.”**

### Why? (Reasoning)

#### A. The cloud “cheats” the network

In CAP theory, a “Partition” (P) is when server A cannot see server B.  
In practice (AWS Region, Google Cloud), intra-datacenter networks are so reliable and redundant that “pure” partitions are extremely rare.

#### B. Infrastructure solves the problem for you

What happens when an entire Availability Zone (one datacenter) goes down?

1. **Load Balancer (LB)** detects the zone is “dead” (Health Check failed).
2. The LB instantly shifts traffic to another zone that has replicas of your service.
3. **Client:** gets a response (Available) and up-to-date data from the replica (Consistent).
4. **Conclusion:** for the client, the system is **CA** (both available and consistent). The “P” problem is handled at the infrastructure layer, not in your code.

#### C. Term confusion

* **CAP-Available:** “Every non-failing node must respond,” even the one that is cut off from the database.
* **Engineering-Available:** “The user can buy a product.”

If we remove the “isolated” node from traffic (via a Load Balancer), we violate formal CAP availability, but we preserve **real business availability**.

---

## 3. The real trade-offs

If CAP isn’t the core concern, what should an architect think about?  
Brooker suggests focusing on **PACELC** (an extension of CAP) and physical metrics.

### 1. Latency vs Consistency

This is the key decision.

* **Scenario:** You post a comment.
* **Strong Consistency:** We wait until the comment is written to 3 datacenters (Kyiv, Frankfurt, London). Reliable, but slow (**Latency 200ms+**).
* **Eventual Consistency:** We write to one (Kyiv) and return “OK.” Replication happens in the background. Fast (**Latency 20ms**), but if Kyiv burns down in 10ms, the comment is lost.

### 2. Durability vs Latency

* Do we `fsync` to disk on every request? (Safe, slow.)
* Or write to memory (Redis) and flush to disk once per second? (Fast, risk losing 1s of data.)

---

## 4. FinOps: the hidden cost of “solving” CAP

Brooker is right: in the cloud, you can get both **Consistency** and **Availability**. But he doesn’t highlight the cost.
To look **CA** to the client (always available, always consistent), you pay an “insurance tax.”

### 1. Redundancy tax

To survive a zone failure (Partition Tolerance), you must keep copies.

* **Scenario:** You use AWS RDS Multi-AZ.
* **Reality:** You pay for **two** servers (Primary + Standby).
* **Efficiency:** The standby server doesn’t process traffic. It just “heats the air,” waiting for an incident.
* **Overpayment:** **100%**. You pay double for the same performance, just to check the “High Availability” box.

### 2. Network tax (cross-AZ traffic cost)

Consistency requires replication.

* **Physics:** To make data reliable, you write it synchronously to Zone A and Zone B.
* **FinOps:** In clouds (AWS/GCP/Azure), traffic *within* a zone is free. Traffic *between* zones is billed (~$0.01/GB).
* **Impact:** For a high-write, high-load database, replication (cross-AZ traffic) can be **up to 30% of the total bill**. You pay for every byte of consistency.

### 3. Globality tax (Global Tables)

If you want to “cheat” CAP at planetary scale (low latency + strong consistency worldwide), you use DynamoDB Global Tables or Google Spanner.

* **Mechanics:** “Write locally, replicate globally”.
* **Cost:** You pay for writes (Write Units) in *every* region.
* **Math:** With 3 regions, one write can cost like **5 writes** (1 original + 2 replications + 2 inter-region traffic components).

> **Engineering conclusion:**
> In the cloud, CAP turns into a financial equation:
> **Availability + Consistency = $$$.**
> The question is not “is it possible,” but “is the business willing to pay for it.”

---

## 5. Engineering takeaways for your project

1. **Don’t start design with CAP.** Don’t say “We’re building an AP system.” That’s a junior marker.
2. **Think in terms of latency.** Ask the business: “Are you willing to wait 500ms on the ‘Buy’ button to guarantee we never oversell a ticket, or should we sell fast and resolve conflicts later?”
3. **Where CAP still matters:**
* **IoT / Mobile:** Your phone in the метро (no network). Here you’re **forced** to choose: let users take notes offline (AP, possible sync conflicts) or lock the screen (CP).
* **Blockchain:** Partition is a fact of life.

---

## 6. Discussion task

**Situation:** You’re building a **“Shopping Cart”** system for Rozetka/Amazon.  
The customer adds an item while riding an elevator (internet disappears).

1. Which CAP strategy do you choose (AP or CP)?
2. What are the business consequences if you choose CP (Consistency)?
3. How do you resolve a merge conflict if the customer added an item on the phone (offline), then removed it on the laptop (online), and then the phone reconnects?

<details>
<summary>Architect’s answer</summary>

1. **Choice:** definitely **AP (Availability)**. The business will never allow blocking the “Buy” button due to bad internet. Money beats perfect consistency.
2. **CP consequences:** the user sees “Error: Network Error” or a spinner, closes the app, and goes to a competitor. Conversion loss.
3. **Merge strategy:** “Last Write Wins” (dangerous) or **“Union”**. Better to restore a deleted item and let the user delete it again than to accidentally delete something they wanted to buy. (Amazon uses DynamoDB with exactly this logic.)
</details>



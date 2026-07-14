# Operator Guide

This guide is for plant operators and technology partners deploying the power plant operations coordination actor.

## Operational workflow

### 1. Plant registration

Before the actor can handle any operations, the plant must be registered in the store:

```clojure
(def store (store/mem-store
  {:plant-001 {:name "Thermal Unit 1"
               :status :operational
               :verified? true
               :location "XYZ Utility, Plant A"
               :capacity-mw 500}}))
```

The `:verified?` flag indicates that a human (qualified operator/engineer) has confirmed the plant record's accuracy. The Governor requires this flag to be `true` for any operation to proceed.

### 2. Submit an operational request

An operator or a telemetry system submits a request:

```clojure
(def request {:plant-id :plant-001
              :op :log-production-reading
              :payload {:timestamp "2026-07-14T10:30:00Z"
                        :mw-output 450.5
                        :temperature-c 65.2
                        :pressure-bar 18.5}})
```

Allowed operations:
- `:log-production-reading` — routine output/parameter reading logging.
- `:schedule-maintenance` — maintenance scheduling proposal.
- `:flag-anomalous-reading` — surface an out-of-range reading; **ALWAYS escalates**.
- `:coordinate-shift-handover` — shift-handover coordination note.

### 3. Run the actor

```clojure
(require '[plant-ops.actor :as actor])

(def graph (actor/build-graph {:store store}))

(def result (actor/run-request! graph request {} "thread-001"))
;; Result: {:state {:record {...} :audit [...]}
;;          :events [...]
;;          :status :done|:interrupted
;;          :frontier ...}
```

**Possible outcomes:**

- **`:status :done`** (no escalation required)
  - The request succeeded and was committed to the store.
  - Check `:state :record` for the persisted record.

- **`:status :interrupted`** (waiting for human approval)
  - The proposal was flagged for escalation (either `:flag-anomalous-reading` or low advisor confidence).
  - A human operator must review the proposal and decide whether to approve or deny.
  - Use `actor/approve!` to resume (see step 4).

- **`:status :hold`** (rejected, no escalation)
  - The Governor rejected the proposal permanently (hard violation).
  - Examples: unregistered plant, non-`:propose` effect, generator-control attempt.
  - No recovery possible; the proposal is discarded.

### 4. Human approval (if interrupted)

If the actor returned `:status :interrupted`, a human must approve:

```clojure
(def approval-result (actor/approve! graph "thread-001"))
;; This resumes the interrupted request and commits it.
```

After approval, the record is committed and the actor returns `:status :done`.

### 5. Audit & compliance

All decisions are logged in the audit ledger:

```clojure
(store/ledger store)
;; Returns: [{:node :advise :request {...} :proposal {...}}
;;           {:node :govern :verdict {...}}
;;           {:disposition :request-approval ...}
;;           {:node :commit :record {...}}]
```

Export the ledger regularly for compliance audits and incident investigation.

## Customizing the Advisor

The default `mock-advisor` is deterministic and suitable for testing. For production:

1. Implement the `Advisor` protocol:
   ```clojure
   (deftype LLMAdvisor [model]
     Advisor
     (-advise [_ store request]
       ;; Call your LLM to propose an action.
       ;; Always return :effect :propose.
       ;; Return :confidence 0.0 on parse failure (forces escalation).
       ))
   ```

2. Pass it to `build-graph`:
   ```clojure
   (actor/build-graph
     {:store store
      :advisor (LLMAdvisor. your-model)})
   ```

## Customizing the Governor

The Governor policy is in `src/plant_ops/governor.cljc`. If your plant has different safety rules:

1. Modify the `:escalate?` logic (e.g., additional ops that require human approval).
2. Modify the `:hard?` violations (e.g., additional prerequisites that always reject).
3. Adjust `confidence-floor` if your advisor has different reliability metrics.
4. Document why the change is needed in a comment or ADR.

**Important:** Do not weaken the hard invariants:
- `:no-plant` — always reject unregistered plants.
- `:no-actuation` — always require `:effect :propose`.
- `:no-generator-control` — always reject generator/turbine/grid-sync operations (licensed operator authority only).

## Integration with external systems

### Telemetry ingestion
Your telemetry system (SCADA, PI Historian, IoT sensors) submits requests to the actor:
```clojure
(actor/run-request! graph
  {:plant-id :plant-001
   :op :log-production-reading
   :payload {:mw-output 450.5 ...}}
  {}
  "telemetry-thread-001")
```

### Maintenance management system (CMMS)
When the actor commits a `:schedule-maintenance` proposal, push it to your CMMS:
```clojure
(when (= :schedule-maintenance (:op (:record state)))
  (cmms/create-work-order (:record state)))
```

### Alerting / escalation
When `:flag-anomalous-reading` is submitted, the actor escalates to human approval:
```clojure
(if (= :interrupted (:status result))
  (send-alert-to-operator "Anomalous reading flagged, awaiting approval"))
```

## Troubleshooting

**Q: My request returned `:status :hold`. Why?**
A: The Governor rejected it as a hard violation. Check the `:verdict` in the audit ledger for details. Common causes:
- Plant not registered.
- Plant not verified.
- Proposal `:effect` is not `:propose`.
- Proposal `:op` is a generator/turbine/grid-sync command (forbidden).

**Q: My request returned `:status :interrupted`. What do I do?**
A: A human operator must review the proposal and call `actor/approve!` to resume. This is the intended flow for escalations (anomalous readings, low-confidence advisors).

**Q: How do I export the audit ledger for compliance?**
A: Call `(store/ledger store)` and serialize to JSON or CSV. The ledger is append-only and tamper-evident.

**Q: Can I integrate with an LLM?**
A: Yes. Implement the `Advisor` protocol and swap `mock-advisor` for your LLM advisor. Always return `:confidence 0.0` on parse failures (forces escalation, never fabricated confidence).

## Further reading

- [`README.md`](../README.md) — project overview and design rationale.
- [`src/plant_ops/governor.cljc`](../src/plant_ops/governor.cljc) — hard/escalation invariants.
- [`src/plant_ops/actor.cljc`](../src/plant_ops/actor.cljc) — StateGraph wiring and flow.

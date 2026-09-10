(ns treatment-ops.governor
  "TreatmentOpsGovernor — the independent safety/traceability layer for
  the ISCO-08 3132 incinerator and water treatment plant operations
  coordination actor. Wired as its own `:govern` node in
  `treatment-ops.actor`'s StateGraph, downstream of `:advise` — the
  Advisor has no notion of plant provenance or operational risk, so this
  MUST be a separate system able to reject a proposal (itonami actor
  pattern, per ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. plant provenance        — the request's plant-id must be registered and verified.
    2. no-actuation            — proposal :effect must be :propose.
    3. no-combustion-control   — proposal :op must NOT be :ignition-control,
                                 :chemical-injection, :thermal-process-start,
                                 or :emergency-shutdown (those are exclusively
                                 licensed operator authority, never automated).

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    4. :op :flag-anomalous-reading  — always escalates (high-safety signal).
    5. low confidence (< `confidence-floor`)."
  (:require [treatment-ops.store :as store]))

(def confidence-floor 0.7)
(def ^:private forbidden-ops
  #{:ignition-control :chemical-injection :thermal-process-start :emergency-shutdown})
(def ^:private escalating-ops #{:flag-anomalous-reading})

(defn- hard-violations [{:keys [proposal]} plant-record]
  (cond-> []
    (nil? plant-record)
    (conj {:rule :no-plant :detail "unregistered or unverified plant"})

    (not (:verified? plant-record))
    (conj {:rule :unverified-plant :detail "plant must be verified before operations"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect must be :propose only (no direct actuation)"})

    (contains? forbidden-ops (:op proposal))
    (conj {:rule :no-combustion-control :detail "ignition control, chemical injection, thermal process start, and emergency shutdown are exclusively licensed operator authority"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `treatment-ops.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [plant-record (store/plant store (:plant-id request))
        hard (hard-violations {:proposal proposal} plant-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))

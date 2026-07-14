(ns treatment-ops.advisor
  "TreatmentOpsAdvisor protocol — advisors for incinerator and water
  treatment plant operations coordination proposals. The Advisor ingests
  a request (plant ID, operation type, telemetry) and returns a proposal
  with :effect :propose. Advisors never directly write state or dispatch
  actuation (the Governor and StateGraph Actor gate that).

  Concrete implementations: `mock-advisor` (deterministic, for testing)
  and `llm-advisor` (wraps an LLM).")

(defprotocol Advisor
  "An advisor for treatment plant operations coordination."
  (-advise [advisor store request]
    "Ingest a request and return a proposal:
    `{:op :log-treatment-reading|:schedule-maintenance|:flag-anomalous-reading|:coordinate-shift-handover
      :payload .. :confidence [0.0..1.0] :effect :propose}`
    Always returns :effect :propose. Never mutates store."))

(defn mock-advisor
  "A deterministic mock advisor for testing. Routes on request :op to
  return a fixed proposal with high confidence."
  []
  (reify Advisor
    (-advise [_ _store request]
      (let [{:keys [op payload]} request]
        {:op op
         :payload payload
         :confidence 0.9
         :effect :propose}))))

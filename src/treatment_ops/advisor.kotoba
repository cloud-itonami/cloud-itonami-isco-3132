(ns treatment-ops.advisor
  "TreatmentOpsAdvisor protocol — advisors for incinerator and water
  treatment plant operations coordination proposals. The Advisor ingests
  a request (plant ID, operation type, telemetry) and returns a proposal
  with :effect :propose. Advisors never directly write state or dispatch
  actuation (the Governor and StateGraph Actor gate that).

  Concrete implementations: `mock-advisor` (deterministic, for testing)
  and `llm-advisor` (wraps an LLM). Either way the advisor ONLY produces
  a PROPOSAL — it never writes to the store; `treatment-ops.governor` is
  the independent system that decides whether the proposal may proceed."
  (:require #?(:clj [clojure.edn :as edn] :cljs [cljs.reader :as edn])))

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

(def ^:private system-prompt
  "You are a treatment-plant operations advisor. Given a request, propose
   an :op, the :payload, and an honest :confidence (0.0-1.0). Never
   fabricate confidence you don't have -- the governor escalates/holds
   on low confidence, and never dispatches actuation itself regardless.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  "Wraps a `langchain.model/ChatModel`. `gen-opts` is passed through to
  `model/-generate`. Kept decoupled from any concrete model so this ns
  has no hard dependency beyond `langchain.model`'s protocol."
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))

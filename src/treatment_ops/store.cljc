(ns treatment-ops.store
  "TreatmentOpsStore protocol — backing store for incinerator and water
  treatment plant operations coordination: registered plants/units,
  committed operation records, and append-only audit ledger.

  A minimal implementation holds all state in memory (MemStore).
  Production implementations will implement the Store protocol against
  a durable backend (e.g. datomic).")

(defprotocol Store
  "A backing data store for treatment plant operations coordination."
  (plant [store plant-id]
    "Look up a registered plant/unit by ID. Returns `{:plant-id ..
    :name .. :status .. :verified? ..}` or nil if not found.")
  (commit-record! [store record]
    "Atomically commit an operation record: `{:plant-id .. :op
    .. :payload ..}`. The store must persist the record before returning.
    Throws on error.")
  (append-ledger! [store entry]
    "Atomically append an audit ledger entry to the immutable ledger.
    Entry shape: `{:disposition :commit|:hold|:escalate :record|:verdict ..}`.")
  (records [store]
    "Return the seq of committed operation records.")
  (ledger [store]
    "Return the full append-only audit ledger (seq of entries)."))

(defrecord MemStore [plants records-ref ledger-ref]
  Store
  (plant [_ plant-id]
    (get plants plant-id))
  (commit-record! [_ record]
    (when-not (:plant-id record)
      (throw (ex-info "commit-record! requires :plant-id" {:record record})))
    (swap! records-ref conj record))
  (append-ledger! [_ entry]
    (swap! ledger-ref conj entry))
  (records [_]
    @records-ref)
  (ledger [_]
    @ledger-ref))

(defn mem-store
  "Create an in-memory MemStore with optional initial plants.
  `plants-map` is `{:plant-id {:name .. :status .. :verified? true}}`."
  ([] (mem-store {}))
  ([plants-map]
   (MemStore. plants-map (atom []) (atom []))))

(ns treatment-ops.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [treatment-ops.governor :as gov]
            [treatment-ops.store :as store]))

(deftest hard-violations-unregistered-plant
  (testing "unregistered plant triggers hard violation"
    (let [proposal {:op :log-treatment-reading :effect :propose}
          request {:plant-id :nonexistent}
          s (store/mem-store {})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))
      (is (not (:ok? verdict)))
      (is (seq (:violations verdict))))))

(deftest hard-violations-unverified-plant
  (testing "unverified plant triggers hard violation"
    (let [proposal {:op :log-treatment-reading :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? false}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))
      (is (not (:ok? verdict))))))

(deftest hard-violations-non-propose-effect
  (testing "non-:propose effect triggers hard violation"
    (let [proposal {:op :log-treatment-reading :effect :commit}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))
      (is (not (:ok? verdict))))))

(deftest hard-violations-forbidden-ops
  (testing "ignition control is forbidden"
    (let [proposal {:op :ignition-control :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "chemical injection is forbidden"
    (let [proposal {:op :chemical-injection :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "thermal process start is forbidden"
    (let [proposal {:op :thermal-process-start :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict))))
  (testing "emergency shutdown is forbidden"
    (let [proposal {:op :emergency-shutdown :effect :propose}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:hard? verdict)))))

(deftest escalation-anomalous-reading
  (testing "flag-anomalous-reading always escalates"
    (let [proposal {:op :flag-anomalous-reading :effect :propose :confidence 0.95}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (:escalate? verdict))
      (is (not (:ok? verdict))))))

(deftest escalation-low-confidence
  (testing "low confidence triggers escalation"
    (let [proposal {:op :log-treatment-reading :effect :propose :confidence 0.5}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (:escalate? verdict))
      (is (not (:ok? verdict))))))

(deftest ok-proposal
  (testing "valid proposal with high confidence is ok"
    (let [proposal {:op :log-treatment-reading :effect :propose :confidence 0.85}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (not (:hard? verdict)))
      (is (not (:escalate? verdict)))
      (is (:ok? verdict)))))

(deftest ok-schedule-maintenance
  (testing "schedule-maintenance proposal is ok with high confidence"
    (let [proposal {:op :schedule-maintenance :effect :propose :confidence 0.8}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:ok? verdict)))))

(deftest ok-shift-handover
  (testing "shift-handover proposal is ok with high confidence"
    (let [proposal {:op :coordinate-shift-handover :effect :propose :confidence 0.9}
          request {:plant-id :plant-1}
          s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          verdict (gov/check request nil proposal s)]
      (is (:ok? verdict)))))

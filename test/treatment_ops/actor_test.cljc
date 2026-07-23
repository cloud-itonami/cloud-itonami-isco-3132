(ns treatment-ops.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [treatment-ops.actor :as actor]
            [treatment-ops.store :as store]))

(defn- run-req! [s request thread-id]
  (actor/run-request! (actor/build-graph {:store s}) request nil thread-id))

(deftest log-treatment-reading-commits
  (testing "a clean proposal against a verified plant commits and appends to the ledger"
    (let [s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          result (run-req! s {:plant-id :plant-1 :op :log-treatment-reading :payload {:reading 42}} (str (random-uuid)))]
      (is (= :done (:status result)))
      (is (= :commit (get-in result [:state :disposition])))
      (is (= 1 (count (store/records s))))
      (is (= 1 (count (store/ledger s)))))))

(deftest unverified-plant-holds
  (testing "an unverified plant is a HARD hold -- never even reaches the approval interrupt"
    (let [s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? false}})
          result (run-req! s {:plant-id :plant-1 :op :log-treatment-reading :payload {:reading 42}} (str (random-uuid)))]
      (is (= :done (:status result)))
      (is (= :hold (get-in result [:state :disposition])))
      (is (empty? (store/records s)))
      (is (= 1 (count (store/ledger s)))))))

(deftest anomalous-reading-always-escalates-and-resumes-to-commit
  (testing "flag-anomalous-reading always escalates (mock-advisor's fixed 0.9 confidence doesn't
  matter -- the governor's always-escalate op list forces human sign-off), and a genuine resume
  via approve! carries it through to commit"
    (let [s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          thread-id (str (random-uuid))
          graph (actor/build-graph {:store s})
          paused (actor/run-request! graph {:plant-id :plant-1 :op :flag-anomalous-reading :payload {:reading 999}} nil thread-id)]
      (is (= :interrupted (:status paused)))
      (is (empty? (store/records s)))
      (let [resumed (actor/approve! graph thread-id)]
        (is (= :done (:status resumed)))
        (is (= :commit (get-in resumed [:state :disposition])))
        (is (= 1 (count (store/records s))))))))

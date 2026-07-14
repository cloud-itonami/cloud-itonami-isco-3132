(ns treatment-ops.store-test
  (:require [clojure.test :refer [deftest is testing]]
            [treatment-ops.store :as store]))

(deftest mem-store-basics
  (testing "create empty mem-store"
    (let [s (store/mem-store)]
      (is (= [] (store/records s)))
      (is (= [] (store/ledger s)))))

  (testing "create mem-store with initial plants"
    (let [plants {:plant-1 {:name "Treatment Plant 1" :status :operational :verified? true}}
          s (store/mem-store plants)]
      (is (store/plant s :plant-1))
      (is (nil? (store/plant s :plant-2)))))

  (testing "commit-record! appends to records"
    (let [s (store/mem-store {:plant-1 {:name "Plant 1" :status :operational :verified? true}})
          record {:plant-id :plant-1 :op :log-treatment-reading :payload {:reading 42}}]
      (store/commit-record! s record)
      (is (= 1 (count (store/records s))))
      (is (= record (first (store/records s))))))

  (testing "commit-record! without plant-id throws"
    (let [s (store/mem-store)
          record {:op :log-treatment-reading :payload {}}]
      (is (thrown? #?(:clj Exception :cljs js/Error) (store/commit-record! s record)))))

  (testing "append-ledger! adds entries"
    (let [s (store/mem-store)
          entry {:disposition :commit :record {:plant-id :plant-1 :op :log-treatment-reading}}]
      (store/append-ledger! s entry)
      (is (= 1 (count (store/ledger s))))
      (is (= entry (first (store/ledger s)))))))

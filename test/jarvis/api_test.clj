(ns jarvis.api-test
  (:require [clojure.test :refer :all]
            [jarvis.api :refer :all])
  (:import (java.util UUID Date)))

(defn- after-or-equals? [date1 date2]
  "Test if date2 is after or equals to date1"
  (or (.after date2 date1)
      (.equals date2 date1)))

(deftest ref-test
  (testing "ref specs - supported creation"
    (let [ref (new-ref :task (new-id))]
      (is (= :task (:type ref)))
      (is (instance? UUID (:id ref)))))
  (testing "ref specs - invalid creation"
    (is (thrown? AssertionError (new-ref :task :az)))))

(deftest event-test
  (testing "event specs - supported creation"
    (let [ref (new-ref :task (new-id))
          nowP (now)
          event (new-event ref :task/created)
          nowA (now)]
      (is (= ref (:ref event)))
      (is (= :task/created (:type event)))
      (is (after-or-equals? nowP (:timestamp event)))
      (is (after-or-equals? (:timestamp event) nowA))
      )))

(deftest event-test
  (testing "event specs - supported creation"
    (let [task (create-task "Definir les TOs"
                            :description "Qui, quoi, quand..."
                            :labels #{:to})]
      (is (= :task (:type (:ref task))))
      (is (= "Definir les TOs" (:name task)))
      (is (= "Qui, quoi, quand..." (:description task)))
      (is (= :normal (:priority task)))
      (is (= :not-started (:status task)))
      (is (= #{:to} (:labels task)))
      )))

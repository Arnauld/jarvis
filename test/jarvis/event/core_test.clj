(ns jarvis.event.core-test
  (require [clojure.test :refer :all]
           [jarvis.event.core :refer :all])
  (:import (jarvis.event.core InMemory)
           (jarvis.event MidAirCollision))
  (:use [clojure.pprint :only [pprint]]))


(deftest in-memory-eventstore
  (testing "append and retrieve events"
    (let [store (-> (InMemory.)
                    (append-events 1 0
                                   [{:type :created}
                                    {:type :name-changed :data {:name "jarvis"}}
                                    {:type :skills-added :data #{:java}}
                                    {:type :skills-added :data #{:clojure :erlang}}
                                    {:type :skills-removed :data #{:java}}]))]
      ;(pprint store)
      (is (= [{:stream-id 1 :version 1 :type :created}]
             (stream-of store 1 {:max-version 1})))
      (is (= [{:stream-id 1 :version 2 :type :name-changed :data {:name "jarvis"}}
              {:stream-id 1 :version 3 :type :skills-added :data #{:java}}]
             (stream-of store 1 {:min-version 2 :max-version 3})))))
  (testing "mid-air-collision when expected-version is invalid"
    (let [store (-> (InMemory.)
                    (append-events 1 0
                                   [{:type :created}
                                    {:type :name-changed :data {:name "jarvis"}}
                                    {:type :skills-added :data #{:java}}]))]
      (pprint store)
      (is (thrown? MidAirCollision
                   (append-events store 1 1
                                  [{:type :skills-added :data #{:clojure :erlang}}]))))))


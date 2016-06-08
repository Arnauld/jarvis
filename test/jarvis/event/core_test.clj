(ns jarvis.event.core-test
  (require [clojure.test :refer :all]
           [jarvis.event.core :refer :all])
  (:import (jarvis.event.core InMemory)
           (jarvis.event MidAirCollision))
  (:use [clojure.pprint :only [pprint]]))


(deftest in-memory-eventstore
  (testing "append and retrieve events"
    (let [store (-> (InMemory.)
                    (append-events 7 0
                                   [{:type :created}
                                    {:type :name-changed :data {:name "jarvis"}}
                                    {:type :skills-added :data #{:java}}
                                    {:type :skills-added :data #{:clojure :erlang}}
                                    {:type :skills-removed :data #{:java}}]))
          stream1 (stream-of store 7 {:max-sequence 1})
          stream2 (stream-of store 7 {:min-sequence 2 :max-sequence 3})]
      ;(pprint store)
      (is (= [{:stream-id 7 :sequence 1 :data {:type :created}}]
             (:events stream1)))
      (is (= [{:stream-id 7 :sequence 2 :data {:type :name-changed :data {:name "jarvis"}}}
              {:stream-id 7 :sequence 3 :data {:type :skills-added :data #{:java}}}]
             (:events stream2)))
      (is (= 5 (:version stream1)))
      (is (= 5 (:version stream2)))
      (is (= 7 (:stream-id stream1)))
      (is (= 7 (:stream-id stream2)))))

  (testing "retrieve events of an unknown stream"
    (let [store (InMemory.)
          stream (stream-of store 7 {:max-version 1})]
      (is (nil? stream))))

  (testing "mid-air-collision when expected-version is invalid"
    (let [store (-> (InMemory.)
                    (append-events 7 0
                                   [{:type :created}
                                    {:type :name-changed :data {:name "jarvis"}}
                                    {:type :skills-added :data #{:java}}]))]
      (pprint store)
      (is (thrown? MidAirCollision
                   (append-events store 7 1
                                  [{:type :skills-added :data #{:clojure :erlang}}]))))))


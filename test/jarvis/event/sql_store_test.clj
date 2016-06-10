(ns jarvis.event.sql-store-test
  (require [clojure.test :refer :all]
           [jarvis.event.core :refer :all]
           [jarvis.event.sql-store :as sql])
  (:import (jarvis.event MidAirCollision)
           (jarvis.event.sql_store SqlStore)))


(def db-spec {:classname   "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname     "//localhost:5432/jarvis-test"
              :user        "postgres"
              :password    "postgres"})

(defn- reset-event-store [db-spec]
  (do
    (sql/drop-schema db-spec)
    (sql/create-schema db-spec)
    (SqlStore. db-spec)))

(deftest sql-eventstore
  (testing "append and retrieve events"
    (sql/drop-schema db-spec)
    (sql/create-schema db-spec)
    (let [store (-> (SqlStore. db-spec)
                    (append-events "7" 0
                                   [{:type :created}
                                    {:type :name-changed :data {:name "jarvis"}}
                                    {:type :skills-added :data #{:java}}
                                    {:type :skills-added :data #{:clojure :erlang}}
                                    {:type :skills-removed :data #{:java}}]))
          stream1 (stream-of store 7 {:max-sequence 1})
          stream2 (stream-of store 7 {:min-sequence 2 :max-sequence 3})]
      (is (= [{:stream-id "7" :sequence 1 :event {:type :created}}]
             (:events stream1)))
      (is (= [{:stream-id "7" :sequence 2 :event {:type :name-changed :data {:name "jarvis"}}}
              {:stream-id "7" :sequence 3 :event {:type :skills-added :data #{:java}}}]
             (:events stream2)))
      (is (= 5 (:version stream1)))
      (is (= 5 (:version stream2)))
      (is (= 7 (:stream-id stream1)))
      (is (= 7 (:stream-id stream2))))



    (testing "retrieve events of an unknown stream"
      (let [store (reset-event-store db-spec)
            stream (stream-of store "7" {:max-version 1})]
        (is (nil? stream))))

    (testing "mid-air-collision when expected-version is invalid"
      (let [store (-> (reset-event-store db-spec)
                      (append-events "7" 0
                                     [{:type :created}
                                      {:type :name-changed :data {:name "jarvis"}}
                                      {:type :skills-added :data #{:java}}]))]
        (is (thrown? MidAirCollision
                     (append-events store "7" 1
                                    [{:type :skills-added :data #{:clojure :erlang}}])))))))

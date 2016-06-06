(ns jarvis.jdbc.usecase-test
  (require [clojure.test :refer :all]
           [jarvis.core :refer :all]
           [clojure.java.jdbc :as jdbc]
           [java-jdbc.ddl :as ddl]
           [java-jdbc.sql :as sql])
  (:import (java.sql SQLException)))

(def db-spec {:classname   "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname     "//localhost:5432/jarvis-test"
              :user        "postgres"
              :password    "postgres"})

(deftest a-test
  (testing "FIXME, I fail not anymore..."
    (try
      (do
        (jdbc/db-do-commands db-spec
                             "DROP TABLE events"
                             "CREATE TABLE events (
                                stream_id character varying NOT NULL,
                                version bigint NOT NULL,
                                event character varying not null,
                                CONSTRAINT pk PRIMARY KEY (stream_id, version))")
        (jdbc/insert! db-spec :events
                      nil                                   ; column names omitted
                      ["id-1" 1 (pr-str {:type :created :when 17})]
                      ["id-1" 2 (pr-str {:type :skills-added :when 18 :skills #{:java}})]
                      ["id-1" 3 (pr-str {:type :skills-added :when 19 :skills #{:clojure :erlang}})]
                      ["id-1" 4 (pr-str {:type :skills-removed :when 20 :skills #{:java}})])
        (is (= [{:stream_id "id-1", :version 1, :event "{:type :created, :when 17}"}
                {:stream_id "id-1", :version 2, :event "{:type :skills-added, :when 18, :skills #{:java}}"}
                {:stream_id "id-1", :version 3, :event "{:type :skills-added, :when 19, :skills #{:clojure :erlang}}"}
                {:stream_id "id-1", :version 4, :event "{:type :skills-removed, :when 20, :skills #{:java}}"}]
               (jdbc/query db-spec
                           (sql/select * :events
                                       (sql/where {:stream_id "id-1"})
                                       (sql/order-by :version))))))
      (catch SQLException e (do
                              (.printStackTrace e)
                              (.printStackTrace
                                (.getNextException e)))))))

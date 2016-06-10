(ns jarvis.event.sql-store
  (require [clojure.java.jdbc :as jdbc]
           [jarvis.event.core :refer :all]
           [java-jdbc.sql :as sql])
  (:import (jarvis.event MidAirCollision)
           (jarvis.event.core Stream)))

(defn drop-schema [db-spec]
  (jdbc/db-do-commands db-spec
                       "DROP TABLE events"))

(defn create-schema [db-spec]
  (jdbc/db-do-commands db-spec
                       "CREATE TABLE events (
                          stream_id character varying NOT NULL,
                          version bigint NOT NULL,
                          event character varying not null,
                          CONSTRAINT pk PRIMARY KEY (stream_id, version))"))

(defn- stream-version [db-spec stream-id]
  (jdbc/query db-spec
              ["select max(version) from events where stream_id = ?" (str stream-id)]
              :row-fn #(or (:max %) 0)
              :result-set-fn first))

(defrecord SqlStore [db-spec]
  EventStore
  (stream-of [store stream-id opts]
    (let [min (or (:min-sequence opts) 0)
          max (or (:max-sequence opts) Integer/MAX_VALUE)
          events (jdbc/query db-spec
                             ["select * from events where stream_id = ? and ? <= version and version <= ? order by version" (str stream-id) min max]
                             :row-fn (fn [r]
                                       {:stream-id (:stream_id r)
                                        :sequence (:version r)
                                        :event (clojure.edn/read-string (:event r))}
                                       ))]
      (println "stream >> [" min "," max "] >>" events)
      (Stream. stream-id (stream-version db-spec stream-id) events)))

  (append-events [store stream-id expected-version events]
    (let [actual-version (stream-version db-spec stream-id)]
      (when-not (= actual-version expected-version)
        (throw
          (MidAirCollision. {:stream-id        stream-id
                             :expected-version expected-version
                             :actual-version   actual-version})))
      (jdbc/with-db-transaction [tx db-spec]
                                (let [stmt (jdbc/prepare-statement (jdbc/db-find-connection tx)
                                                                   "insert into events (stream_id, version, event) values (?,?,?)")]
                                  (reduce
                                    (fn [sequence event]
                                      (do
                                        (.setString stmt 1 (str stream-id))
                                        (.setLong stmt 2 (long sequence))
                                        (.setString stmt 3 (prn-str event))
                                        (.execute stmt)
                                        (inc sequence)))
                                    (inc actual-version)
                                    events)))
      store)))

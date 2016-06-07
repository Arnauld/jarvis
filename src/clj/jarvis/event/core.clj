(ns jarvis.event.core
  (:import (jarvis.event MidAirCollision)))

(defprotocol EventStore
  (stream-of [store stream-id opts])
  (append-events [store stream-id expected-version events]))

(def empty-stream {:version 0 :events []})

(defrecord InMemory []
  EventStore
  (stream-of [store stream-id opts]
    (let [min (or (:min-version opts) 0)
          max (or (:max-version opts) Integer/MAX_VALUE)]
      (->> (get-in store [stream-id :events])
           (filter (fn [event] (and (<= min (:version event))
                                    (>= max (:version event))))))))
  (append-events [store stream-id expected-version events]
    (reduce (fn [store event]
              (let [stream (get store stream-id empty-stream)
                    version (:version stream)
                    new-version (inc version)
                    new-event (assoc event :version new-version
                                           :stream-id stream-id)
                    new-events (conj (:events stream) new-event)
                    new-stream (assoc stream :events new-events
                                             :version new-version)]
                (when-not (= version expected-version)
                  (MidAirCollision.
                    (str {:stream-id        stream-id
                          :expected-version expected-version
                          :actual-version   version})))
                (assoc store stream-id new-stream)))
            store
            events)))


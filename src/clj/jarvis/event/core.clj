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
    (let [stream (get store stream-id empty-stream)
          actual-version (:version stream)
          [new-events new-version] (reduce (fn [[events event-version] event]
                                             [(conj events
                                                    (assoc event :version (inc event-version)
                                                                 :stream-id stream-id)) (inc event-version)])
                                           [(:events stream) actual-version]
                                           events)
          new-stream (assoc stream :events new-events
                                   :version new-version)]
      (when-not (= actual-version expected-version)
        (throw
          (MidAirCollision. {:stream-id        stream-id
                             :expected-version expected-version
                             :actual-version   (:version stream)})))
      (assoc store stream-id new-stream))))


(ns jarvis.event.core
  (:import (jarvis.event MidAirCollision)))

(defprotocol EventStore
  (stream-of [store stream-id opts])
  (append-events [store stream-id expected-version events]))

(defrecord Stream [stream-id version events])

(def empty-stream {:version 0 :events []})

(defrecord InMemory []
  EventStore
  (stream-of [store stream-id opts]
    (let [min (or (:min-sequence opts) 0)
          max (or (:max-sequence opts) Integer/MAX_VALUE)
          stream (get store (str stream-id))]
      (if stream
        (Stream. (str stream-id)
                 (:version stream)
                 (->> (:events stream)
                      (filter (fn [event] (and (<= min (:sequence event))
                                               (>= max (:sequence event)))))))
        nil)))
  (append-events [store stream-id expected-version events]
    (let [stream (get store (str stream-id) empty-stream)
          actual-version (:version stream)
          [new-events new-version] (reduce (fn [[events event-version] event]
                                             [(conj events
                                                    {:stream-id (str stream-id)
                                                     :sequence  (inc event-version)
                                                     :data      event}) (inc event-version)])
                                           [(:events stream) actual-version]
                                           events)
          new-stream (Stream. (str stream-id) new-version new-events)]
      (when-not (= actual-version expected-version)
        (throw
          (MidAirCollision. {:stream-id        (str stream-id)
                             :expected-version expected-version
                             :actual-version   (:version stream)})))
      (assoc store (str stream-id) new-stream))))


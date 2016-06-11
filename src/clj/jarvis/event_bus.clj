(ns jarvis.event-bus)


(defprotocol EventBus
  (publish! [bus event])
  (register! [bus listener])
  (unregister! [bus listener]))

(defrecord RefBased [listeners]
  EventBus
  (publish! [bus event]
      (doall
        (map #(% event) @listeners)))
  (register! [bus listener]
    (dosync
      (alter listeners conj listener)))
  (unregister! [bus listener]
    (dosync
      (alter listeners disj listener))))

(defn create-in-memory []
  (RefBased. (ref #{})))



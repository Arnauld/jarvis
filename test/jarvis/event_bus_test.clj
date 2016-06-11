(ns jarvis.event-bus-test
  (:require [clojure.test :refer :all]
            [jarvis.event-bus :refer :all]))

(defn create-listener [atom-ref]
  (fn [event] (reset! atom-ref event)))

(deftest in-memory-event-bus
  (testing "register listeners and publish..."
    (let [
          bus (create-in-memory-event-bus)
          listener-ref1 (atom nil)
          listener-ref2 (atom nil)
          listener1 (create-listener listener-ref1)
          listener2 (create-listener listener-ref2)]
      ; --- 1st round
      (register! bus listener1)
      (register! bus listener2)
      (publish! bus {:event/type :create})
      (is (= {:event/type :create} @listener-ref1))
      (is (= {:event/type :create} @listener-ref2))

      ; --- 2nd round
      (unregister! bus listener2)
      (publish! bus {:event/type :delete})
      (is (= {:event/type :delete} @listener-ref1))
      (is (= {:event/type :create} @listener-ref2))))

  (testing "register listener on different bus..."
    (let [
          bus1 (create-in-memory-event-bus)
          bus2 (create-in-memory-event-bus)
          listener-ref1 (atom nil)
          listener-ref2 (atom nil)
          listener1 (create-listener listener-ref1)
          listener2 (create-listener listener-ref2)]
      ; --- 1st round
      (register! bus1 listener1)
      (register! bus2 listener2)
      (publish! bus1 {:event/type :create})
      (is (= {:event/type :create} @listener-ref1))
      (is (= nil @listener-ref2)))))

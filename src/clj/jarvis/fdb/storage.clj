(ns jarvis.fdb.storage
  (:use [clojure.pprint :only [pprint]]))

(defprotocol Storage
  (get-entity [storage e-id])
  (write-entity [storage entity])
  (drop-entity [storage entity]))

(defprotocol Dumpable
  (dump [storage]))

(defrecord InMemory []
  Storage
  (get-entity [storage e-id] (get storage e-id))
  (write-entity [storage entity] (assoc storage (:id entity) entity))
  (drop-entity [storage entity] (dissoc storage (:id entity)))
  Dumpable
  (dump [storage]
    (pprint storage)))

(defrecord Intercept [delegate intercept-fn]
  Storage
  (get-entity [storage e-id]
    (intercept-fn storage
                  :get-entity
                  (fn [] (get-entity (:delegate storage) e-id))))
  (write-entity [storage entity]
    (intercept-fn storage
                  :write-entity
                  (fn []
                    (let [new-delegate (write-entity (:delegate storage) entity)]
                      (Intercept. new-delegate intercept-fn)))))
  (drop-entity [storage entity]
    (intercept-fn storage
                  :drop-entity
                  (fn []
                    (let [new-delegate (drop-entity (:delegate storage) entity)]
                      (Intercept. new-delegate intercept-fn)))))
  Dumpable
  (dump [storage]
    (intercept-fn storage
                  :dump
                  (fn [] (dump (:delegate storage))))))

(ns jarvis.fdb.storage)

(defprotocol Storage
  (get-entity [storage e-id])
  (write-entity [storage entity])
  (drop-entity [storage entity]))

(defrecord InMemory []
  Storage
  (get-entity [storage e-id] (get storage e-id))
  (write-entity [storage entity] (assoc storage (:id entity) entity))
  (drop-entity [storage entity] (dissoc storage (:id entity))))

(defrecord Intercept [delegate intercept-fn]
  Storage
  (get-entity [storage e-id]
    (intercept-fn :get-entity
                  (fn [] (get-entity (:delegate storage) e-id))))
  (write-entity [storage entity]
    (intercept-fn :write-entity
                  (fn []
                    (let [new-delegate (write-entity (:delegate storage) entity)]
                      (Intercept. new-delegate intercept-fn)))))
  (drop-entity [storage entity]
    (intercept-fn :drop-entity
                  (fn []
                    (let [new-delegate (drop-entity (:delegate storage) entity)]
                      (Intercept. new-delegate intercept-fn))))))

(ns jarvis.fdb.usecases-test
  (:require [clojure.test :refer :all]
            [jarvis.fdb.core :as db]
            [jarvis.fdb.constructs :as c]
            [jarvis.fdb.graph :as g]
            [jarvis.fdb.query :as q]
            [jarvis.fdb.manage :as m]
            [jarvis.fdb.storage :as s])
  (:import (jarvis.fdb.storage Intercept InMemory))
  (:use [clojure.pprint :only [pprint]]))


(deftest a-usecase
  (testing "FIXME, I fail."
    (let [db-name "persons"
          __ (m/drop-db-conn db-name)
          __ (m/register-db db-name (c/make-db (Intercept.
                                                 (InMemory.)
                                                 (fn [storage what chain]
                                                   (println ">>" what)
                                                   (let [n (chain)]
                                                     (if (= :write-entity what)
                                                       (s/dump (:delegate storage)))
                                                     n)))))
          db-conn (m/get-db-conn db-name)
          __ (println "db::" db-conn)
          ;
          jarvis (-> (c/make-entity :jarvis)
                     (c/add-attr (c/make-attr :person/name "jarvis" :string))
                     (c/add-attr (c/make-attr :person/labels #{:assistant :virtual} :string :cardinality :db/multiple)))
          __ (db/transact db-conn
                          (db/add-entity jarvis))
          entity0 (c/entity-at (m/db-from-conn db-conn) :jarvis)
          ;;
          __ (db/transact db-conn
                          (db/update-entity :jarvis :person/labels #{:operator} :db/add))
          entity1 (c/entity-at (m/db-from-conn db-conn) :jarvis)
          ;;
          __ (db/transact db-conn
                          (db/update-entity :jarvis :person/labels #{:bot :virtual} :db/reset-to))
          entity2 (c/entity-at (m/db-from-conn db-conn) :jarvis)
          ;;
          __ (db/transact db-conn
                          (db/update-entity :jarvis :person/labels #{:virtual} :db/remove))
          entity3 (c/entity-at (m/db-from-conn db-conn) :jarvis)
          ;;
          __ (db/transact db-conn
                          (db/update-entity :jarvis :person/skills #{:organization}))
          entity4 (c/entity-at (m/db-from-conn db-conn) :jarvis)
          ;;
          __ (pprint (m/db-from-conn db-conn))
          ]
      (is (= "jarvis" (c/get-attr entity0 :person/name)))
      (is (= #{:assistant :virtual} (c/get-attr entity0 :person/labels)))
      (is (= "jarvis" (c/get-attr entity1 :person/name)))
      (is (= #{:assistant :virtual :operator} (c/get-attr entity1 :person/labels)))
      (is (= #{:bot :virtual} (c/get-attr entity2 :person/labels)))
      (is (= #{:bot} (c/get-attr entity3 :person/labels)))
      (is (= #{:organization} (c/get-attr entity4 :person/skills)))
      )))

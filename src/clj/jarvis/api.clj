(ns jarvis.api
  (require [clojure.spec :as s]
           [jarvis.event-bus :as event-bus])
  (:import (java.util UUID)))

(def event-bus (event-bus/create-in-memory))

(def event-store ())

(defn next-id []
  (UUID/randomUUID))

(defn schedule-reminder [ref schedule]
  (println "scheduling" ref "in" schedule))

;;---------------------------------------------------------
;;
;; Ref
;;
;;---------------------------------------------------------

(s/def :jarvis.api.ref/id string?)
(s/def :jarvis.api.ref/type #{:task :person :event :comment})
(s/def ::ref (s/keys :req-un [:jarvis.api.ref/type :jarvis.api.ref/id]))

(defrecord Ref [type id])

(defn new-ref [type id]
  {:post [(s/valid? ::ref %)]}
  (Ref. type id))

;;---------------------------------------------------------
;;
;; TASK
;;
;;---------------------------------------------------------

(s/def :task/status #{:terminated
                      :cancelled
                      :delayed
                      :pending
                      :in-progress
                      :not-started})
(s/def :task/name string?)
(s/def :task/description string?)

(defn should-schedule-task? [task-status]
  (not (contains? [:terminated :cancelled] task-status)))

(defn new-task [^String name
                ^String description
                & {:keys [sub-task-of reminder priority status] :or {priority    100
                                                                     sub-task-of nil
                                                                     status      :status/not-started}}]
  (let [task-ref (Ref. :task (next-id))
        task {:ref              task-ref
              :task/name        name
              :task/description description
              :task/priority    priority
              :task/status      status
              :linked-to        (if sub-task-of
                                  [{:task/ref sub-task-of :relation :subtask}]
                                  [])}
        schedule (or reminder :time/tomorrow)]
    (when (and (should-schedule-task? status)
               (not (= schedule :time/none)))
      (schedule-reminder task-ref schedule))
    task-ref))
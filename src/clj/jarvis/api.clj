(ns jarvis.api
  (require [clojure.spec :as s]
           [jarvis.event-bus :as event-bus])
  (:import (java.util UUID Date)))

(def event-bus (event-bus/create-in-memory))

(def event-store ())

(defn new-id []
  (UUID/randomUUID))

(defn now []
  (Date.))

(defn publish [events]
  (println "publishing" events))

(defn schedule-reminder [ref schedule]
  (println "scheduling" ref "in" schedule))

(defn- validate-or-fail [spec what]
  (let [parsed (s/conform spec what)]
    (if (= parsed ::s/invalid)
      (let [explanation (s/explain-data spec what)]
        (throw (ex-info (str "Invalid input: " explanation) explanation)))
      what)))

;;---------------------------------------------------------
;;
;; Ref
;;
;;---------------------------------------------------------

(s/def :jarvis.ref/id uuid?)
(s/def :jarvis.ref/type #{:task :person :event :comment})
(s/def :jarvis.ref/spec (s/keys :req-un [:jarvis.ref/type :jarvis.ref/id]))

(defrecord Ref [type id])

(defn new-ref [type id]
  {:post [(s/valid? :jarvis.ref/spec %)]}
  (Ref. type id))

;;---------------------------------------------------------
;;
;; EVENT
;;
;;---------------------------------------------------------

(s/def :jarvis.event/ref :jarvis.ref/spec)
(s/def :jarvis.event/type keyword?)
(s/def :jarvis.event/timestamp inst?)
(s/def :jarvis.event/payload #(not (nil? %)))
(s/def :jarvis.event/spec (s/keys :req-un [:jarvis.event/ref :jarvis.event/type :jarvis.event/timestamp :jarvis.event/payload]))

(defrecord Event [ref type timestamp payload])

(defn new-event [ref type
                 & {:keys [payload timestamp] :or {payload {} timestamp (now)}}]
  (validate-or-fail :jarvis.event/spec (Event. ref type timestamp payload)))

;;---------------------------------------------------------
;;
;; TASK
;;
;;---------------------------------------------------------

(s/def :jarvis.task/ref :jarvis.ref/spec)
(s/def :jarvis.task/name string?)
(s/def :jarvis.task/description string?)
(s/def :jarvis.task/status #{:terminated
                             :cancelled
                             :delayed
                             :pending
                             :in-progress
                             :not-started})
(s/def :jarvis.task/labels (s/coll-of keyword? #{}))
(s/def :jarvis.task/priority #{:low :normal :high})
(s/def :jarvis.task/spec (s/keys :req-un [:jarvis.task/ref :jarvis.task/name]
                                 :opt-un [:jarvis.task/description :jarvis.task/status :jarvis.task/priority]))

(defrecord Task [ref name description status priority labels])
(defrecord TaskCreated [task-ref task when])
(defrecord TaskScheduled [task-ref schedule when])

(defn new-task [ref name
                & {:keys [description status priority labels]
                   :or   {description ""
                          status      :not-started
                          priority    :normal
                          labels      #{}}}]
  (validate-or-fail :jarvis.task/spec (Task. ref name description status priority labels)))

(defn should-schedule-task? [task-status]
  (not (contains? [:terminated :cancelled] task-status)))

(defn schedule-task [task schedule]
  (if (and (should-schedule-task? (:status task))
           (not= :none schedule))
    (do
      (schedule-reminder (:ref task) schedule)
      [(TaskScheduled. (:ref task) schedule (now))])
    []))

(defn create-task [^String name
                   & details]
  (let [{:keys [reminder]
         :or   {reminder :none}} details
        ref (new-ref :task (new-id))
        task (apply new-task ref name details)
        events (-> []
                   (conj (TaskCreated. ref task (now)))
                   (into (schedule-task task reminder)))]
    (publish events)
    task))
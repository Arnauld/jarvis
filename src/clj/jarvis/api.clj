(ns jarvis.api
  (require [jarvis.event-bus :as event-bus]))

(def event-bus (event-bus/create-in-memory))

(defn new-task [^String name ^String description & {:keys }])
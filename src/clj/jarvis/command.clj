(ns jarvis.command)

(defn create-task [event-bus
                   ^String name ^String description]
  (publish event-bus [{:type :task/created
                       :name name
                       :description description}])

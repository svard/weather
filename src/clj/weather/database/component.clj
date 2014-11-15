(ns weather.database.component
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [monger.core :as mg]))

(timbre/refer-timbre)

(defrecord MongoDb [conf]
  component/Lifecycle
  (start [component]
    (let [conn (mg/connect {:host (:db-host conf) :port 27017})
          db (mg/get-db conn (:db-name conf))]
      (info "Connecting to db" (format "%s@%s" (:db-name conf) (:db-host conf)))
      (assoc component :db db)))

  (stop [component]
    (info "Disconnecting db")
    (assoc component :db nil)))

(defn new-mongo-db [conf]
  (map->MongoDb {:conf conf}))

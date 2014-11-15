(ns weather.core
  (:require [com.stuartsierra.component :as component]
            [weather.http.component :as http]
            [weather.database.component :as db]
            [weather.messagebus.component :as mb]
            [weather.websocket.component :as ws])
  (:gen-class))

(def weather-conf
  {:port 8085
   :db-host "192.168.0.108"
   :db-name "services_db"
   :mb-host "192.168.0.108"
   :mb-exchange "automation"
   :temp-routing-key "temps"
   :rain-routing-key "rain"
   :redis-host "192.168.0.108"})

(defn get-system [conf]
  (component/system-map
     :db (db/new-mongo-db conf)
     :channels (mb/new-messagebus-channels)
     :redis (ws/new-redis-store conf)
     :mb (component/using
          (mb/new-messagebus conf)
          [:channels])
     :websocket (component/using
                 (ws/new-websocket)
                 [:channels :redis])
     :server (component/using
              (http/new-http-server conf)
              [:db :websocket])))

(defn -main [& args]
  (component/start (get-system weather-conf)))

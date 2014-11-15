(ns weather.http.component
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.core.async :refer [go-loop <!]]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [clojure.java.io :refer [resource]]
            [weather.http.rest :as rest]))

(timbre/refer-timbre)
(timbre/set-config!
 [:appenders :rotor]
 {:min-level :info
  :enabled? true
  :async? false
  :max-message-per-msecs nil
  :fn rotor/appender-fn})
(timbre/set-config!
  [:shared-appender-config :rotor]
  {:path "/var/log/weather.log" :max-size (* 512 1024) :backlog 5})

(defn make-resource [resource db]
  (fn [req]
    (resource db req)))

(defrecord HttpServer [conf db websocket]
  component/Lifecycle
  (start [component]
    (info (str "Starting web server on port " (:port conf)))
    (defroutes app-routes
      (GET "/" [] (resource "index.html"))
      (GET "/temperature/stats" [] (make-resource rest/month-temp (:db db)))
      (GET "/temperature/line" [] (make-resource rest/month-line (:db db)))
      (GET "/seasons" [] (make-resource rest/seasons (:db db)))
      (GET "/precipitation" [] (partial rest/precipitation (:db db)))
      (GET "/chsk" req ((:ajax-get-or-ws-handshake-fn websocket) req))
      (POST "/chsk" req ((:ajax-post-fn websocket) req))
      (route/resources "/")
      (route/not-found "Not Found"))
    (let [ring-handler (-> app-routes
                           (handler/api))
          server (run-server ring-handler {:port (:port conf)})]
      (assoc component :server server)))
  
  (stop [component]
    (info "Stopping web server")
    (when-not (nil? (:server component))
      (:server component :timeout 100)
      (assoc component :server nil))))

(defn new-http-server [conf]
  (map->HttpServer {:conf conf}))

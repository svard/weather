(ns weather.websocket.websocket
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.core.match :refer [match]]
            [taoensso.timbre :as timbre]
            [weather.websocket.redis :as redis]
            [weather.utils.math :as math]))

(timbre/refer-timbre)

(defn user-id [req]
  (let [uid (str (java.util.UUID/randomUUID))]
    (debug "Connected" (:remote-addr req))
    uid))

(defn get-aggregate [data]
  (let [total (:total data)
        count (:count data)
        avg (math/round (/ total count))]
    (-> data
        (dissoc :total :count)
        (assoc :avg avg))))

(defn live-stats [chsk-send! clients]
  (fn [raw-data]
    (let [data (get-aggregate raw-data)]
      (doseq [client (:any @clients)]
        (case (:type data)
          "Indoor" (chsk-send! client [:live-temp/indoor data])
          "Outdoor" (chsk-send! client [:live-temp/outdoor data]))))))

(defn start-loop [ch spec f]
  (go-loop []
    (let [msg (<! ch)
          data (redis/create-or-update spec msg)]
      (f data)
      (recur))))

(defn make-event-handler [spec]
  (fn [{:keys [event ?reply-fn]}]
    (match event
           [:live-temp/init :indoor] (?reply-fn (get-aggregate (get spec (redis/keyname "Indoor"))))
           [:live-temp/init :outdoor] (?reply-fn (get-aggregate (get spec (redis/keyname "Outdoor"))))
           :else ())))

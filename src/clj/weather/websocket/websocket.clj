(ns weather.websocket.websocket
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.core.match :refer [match]]
            [taoensso.timbre :as timbre]
            [weather.websocket.key-store :as key-store]
            [weather.utils.math :as math]
            [cluster-map.core :as cmap :refer [with-connection]]))

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

(defn start-loop [ch store f]
  (go-loop []
    (let [msg (<! ch)
          data (key-store/create-or-update store msg)]
      (f data)
      (recur))))

(defn make-event-handler [store]
  (fn [{:keys [event ?reply-fn]}]
    (with-connection [conn (cmap/connect store)
                      indoor (cmap/cluster-map conn (key-store/keyname "Indoor"))
                      outdoor (cmap/cluster-map conn (key-store/keyname "Outdoor"))]
      (match event
             [:live-temp/init :indoor] (?reply-fn (get-aggregate (get indoor :weather)))
             [:live-temp/init :outdoor] (?reply-fn (get-aggregate (get outdoor :weather)))
             :else ()))))

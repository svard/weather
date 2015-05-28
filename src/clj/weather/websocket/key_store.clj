(ns weather.websocket.key-store
  (:require [clj-time.format :as f]
            [clj-time.local :as l]
            [cluster-map.core :as cmap :refer [with-connection]]
            [weather.utils.math :as math]))

(def date-format (comp (partial f/unparse (f/formatter "yyyy-MM-dd"))
                       l/to-local-date-time))

(defn keyname [device]
  (str (date-format (l/local-now)) ":" device))

(defn- keyname-from-msg [msg]
  (str (date-format (:date msg)) ":" (:deviceName msg)))

(defn- create-key! [conn cluster db msg]
  (let [value {:date (:date msg)
               :current (:temperature msg)
               :total (:temperature msg)
               :count 1
               :high (:temperature msg)
               :low (:temperature msg)
               :type (:deviceName msg)}]
    (cmap/create-db conn db 129600)
    (assoc! cluster :weather value)
    value))

(defn- update-key! [cluster msg]
  (let [current-value (get cluster :weather)
        new-value {:date (:date msg)
                   :current (:temperature msg)
                   :total (math/round (+ (:total current-value) (:temperature msg)))
                   :count (inc (:count current-value))
                   :high (if (< (:high current-value) (:temperature msg))
                           (:temperature msg)
                           (:high current-value))
                   :low (if (> (:low current-value) (:temperature msg))
                          (:temperature msg)
                          (:low current-value))
                   :type (:deviceName msg)}]
    (assoc! cluster :weather new-value)
    new-value))

(defn create-or-update [store msg]
  (let [db (keyname-from-msg msg)]
    (with-connection [conn (cmap/connect store)
                      cluster (cmap/cluster-map conn db)]
      (if (cmap/exist? conn db)
        (update-key! cluster msg)
        (create-key! conn cluster db msg)))))

(ns weather.websocket.redis
  (:require [taoensso.timbre :as timbre]
            [taoensso.carmine :as car :refer (wcar)]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [weather.utils.math :as math]))

(timbre/refer-timbre)

(def date-format (comp (partial f/unparse (f/formatter "yyyy-MM-dd"))
                       l/to-local-date-time))

;(def redis-conn {:pool {} :spec {:host "192.168.0.108" :port 6379}})

(defn make-transient [spec & {:keys [expire]}]
  (reify
    clojure.lang.ILookup
    (valAt [this k]
      (when-let [res (wcar spec (car/get k))]
        res))
    (valAt [this k default]
      (or (.valAt this k) default))
    clojure.lang.ITransientMap
    (assoc [this k v]
      (wcar spec (car/set k v))
      (when expire
        (wcar spec (car/expire k expire)))
      this)
    (without [this k]
      (wcar spec (car/del k))
      this)))

(defn- exists?
  [spec key]
  (not (nil? (get spec key))))

(defn keyname
  [device]
  (str (date-format (l/local-now)) ":" device))

(defn- keyname-from-msg
  [msg]
  (str (date-format (:date msg)) ":" (:deviceName msg)))

(defn- create-key!
  [spec key msg]
  (let [value {:date (:date msg)
               :current (:temperature msg)
               :total (:temperature msg)
               :count 1
               :high (:temperature msg)
               :low (:temperature msg)
               :type (:deviceName msg)}]
    (assoc! spec key value)
    value))

(defn- update-key!
  [spec key msg]
  (let [current-value (get spec key)
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
    (assoc! spec key new-value)
    new-value))

(defn create-or-update
  [spec msg]
  (let [key (keyname-from-msg msg)]
    (if (exists? spec key)
      (update-key! spec key msg)
      (create-key! spec key msg))))

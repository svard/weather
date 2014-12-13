(ns weather.http.rest
  (:require [liberator.core :refer [defresource]]
            [clojure.string :as string]
            [monger.collection :as coll :exclude [update]]
            [monger.query :as query]
            [monger.core :as mg]
            [monger.joda-time]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.local :as l]
            [taoensso.timbre :as timbre]
            [taoensso.carmine :as car :refer (wcar)]
            [weather.utils.math :as math]
            [weather.utils.date :as date]))

(timbre/refer-timbre)

(def conn (mg/connect {:host "192.168.0.108" :port 27017}))
(def db (mg/get-db conn "services_db"))
(declare avg-temp)
(def xf (map #(:temperature %1)))
(def days-in-month [31 28 31 30 31 30 31 31 30 31 30 31])
(def default-formatter (f/formatters :date-time-no-ms))
(def date-formatter (f/formatter "yyyy-MM-dd"))
(def partition-by-date (partition-by #(first (string/split (:dateStr %) #"\s"))))
(def avg-temp-per-day
  (map (fn [xs]
         (let [date (first (string/split (:dateStr (first xs)) #"\s"))]
           {:avg (avg-temp xs)
            :date date}))))

(defn- avg-temp [xs]
  {:pre [(seq xs)]}
  (math/round (/ (transduce xf + 0 xs) (count xs))))

(defn- max-temp [xs]
  {:pre [(seq xs)]}
  (:temperature (apply max-key :temperature xs)))

(defn- min-temp [xs]
  {:pre [(seq xs)]}
  (:temperature (apply min-key :temperature xs)))

(defn- get-temp-range [db range-fn]
  (let [[start end] (range-fn)]
    (query/with-collection db "temperatures"
      (query/find {:date {"$lt" end, "$gte" start}})
      (query/fields [:temperature :dateStr :date :deviceId :deviceName]))))

(defn- prepare-temp-response
  ([db year month]
     (let [range-fn (partial date/get-month-start-end-timestamp year month)]
       (map #(update-in % [:_id] str) (get-temp-range db range-fn))))
  ([db year]
     (let [range-fn (partial date/get-year-start-end-timestamp year)]
       (map #(update-in % [:_id] str) (get-temp-range db range-fn)))))

(defn- get-temp-resource [db params]
  (let [year (read-string (:year params))
        month (read-string (:month params "0"))]
    (if (= month 0)
      (prepare-temp-response db year)
      (prepare-temp-response db year month))))

(defresource month-temp [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [entity (get-temp-resource db params)]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok (fn [ctx]
               {:avg (avg-temp (:entity ctx))
                :max (max-temp (:entity ctx))
                :min (min-temp (:entity ctx))}))

(defn- get-prec-by-date
  ([db year]
     (coll/aggregate db
                     "rainrates"
                     [{"$project" {:year {"$year" "$date"} :month {"$month" "$date"} :rain "$rainrate"}}
                      {"$match" {:year year}}
                      {"$group" {:_id "$month" :sum {"$sum" "$rain"}}}]))
  ([db year month]
     (coll/aggregate db
                     "rainrates"
                     [{"$project" {:year {"$year" "$date"} :month {"$month" "$date"} :rain "$rainrate"}}
                      {"$match" {:year year :month month}}
                      {"$group" {:_id 0 :sum {"$sum" "$rain"}}}]))
  ([db year month day]
     (coll/aggregate db
                     "rainrates"
                     [{"$project" {:year {"$year" "$date"} :month {"$month" "$date"} :day {"$dayOfMonth" "$date"} :rain "$rainrate"}}
                      {"$match" {:year year :month month :day day}}
                      {"$group" {:_id 0 :sum {"$sum" "$rain"}}}])))

(defn- get-humidity [db]
  (query/with-collection db "rainrates"
    (query/find {})
    (query/sort {:date -1})
    (query/fields [:date :humidity])
    (query/limit 1)))

(defn- get-prec-resource [db params]
  (let [year (read-string (:year params))
        month (read-string (:month params))
        [_ _ day] (date/today) 
        monthly (first (get-prec-by-date db year month))
        current (first (get-prec-by-date db year month day))
        humidity (first (get-humidity db))]
    {:current (math/round (:sum current))
     :month (math/round (:sum monthly))
     :humidity (:humidity humidity)
     :date (f/unparse default-formatter (:date humidity))}))

(defresource precipitation [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [entity (get-prec-resource db params)]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok :entity)

(defn- line-data [db params]
  (let [year (read-string (:year params))
        month (read-string (:month params))]
    (->> (prepare-temp-response db year month)
         (map #(assoc {} :date (* 1000 (:date %1)) :temp (:temperature %1))))))

(defresource month-line [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [entity (line-data db params)]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok :entity)

;; (defn- partition-by-date [xs]
;;   (partition-by #(first (string/split (:dateStr %) #"\s")) xs))

;; (defn- partition-by-year [db year]
;;   (let [selection (get-temp-resource db {:year (str year)})]
;;     (partition-by-date selection)))

;; (defn- avg-temp-per-day [db year]
;;   (map (fn [xs]
;;          (let [date (first (string/split (:dateStr (first xs)) #"\s"))]
;;            {:avg (avg-temp xs)
;;             :date date}))
;;        (partition-by-year db year)))

(defn avg-temp-sequence [db year]
  (sequence (comp partition-by-date avg-temp-per-day)
            (get-temp-resource db {:year (str year)})))

(defn- filter-dates-before [year month day date]
  (let [limit (t/date-time year month day)
        current (f/parse date-formatter (:date date))]
    (t/before? limit current)))

(defn- valid-spring-dates [year]
  (partial filter-dates-before year 2 14))

(defn- valid-autumn-dates [year]
  (partial filter-dates-before year 8 1))

(defn- get-season-dates [db year]
  (let [spring-candidates (valid-spring-dates year)
        autumn-candidates (valid-autumn-dates year)
;        temps (avg-temp-per-day db year)
        temps (avg-temp-sequence db year)
        spring-pred (fn [xs]
                      (filter #(<= 0 (:avg %)) xs))
        summer-pred (fn [xs]
                      (filter #(<= 10 (:avg %)) xs))
        autumn-pred (fn [xs]
                      (filter #(>= 10 (:avg %)) xs))
        winter-pred (fn [xs]
                      (filter #(>= 0 (:avg %)) xs))]
    {:year year
     :spring (:date (->> temps
                      (filter spring-candidates)
                      (partition 7 1)
                      (map spring-pred)
                      (filter #(= 7 (count %)))
                      (first)
                      (first)))
     :summer (:date (->> temps
                      (filter spring-candidates)
                      (partition 5 1)
                      (map summer-pred)
                      (filter #(= 5 (count %)))
                      (first)
                      (first)))
     :autumn (:date (->> temps
                      (filter autumn-candidates)
                      (partition 5 1)
                      (map autumn-pred)
                      (filter #(= 5 (count %)))
                      (first)
                      (first)))
     :winter (:date (->> temps
                      (filter autumn-candidates)
                      (partition 5 1)
                      (map winter-pred)
                      (filter #(= 5 (count %)))
                      (first)
                      (first)))}))

(defresource seasons [db req]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [current-year (t/year (l/local-now))]
               {:entity (for [year (range 2013 (inc current-year))]
                          (get-season-dates db year))}))
  :handle-ok (fn [ctx]
               (vec (:entity ctx))))

(ns weather.http.helpers
  (:require [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.local :as l]
            [clojure.core.reducers :as r]
            [weather.utils.math :as math]
            [weather.utils.date :as date]))

(declare average-by)
(declare paverage-by)

(def datetime-format (f/formatters :date-time-no-ms))
(def date-format (f/formatters :date))

(defn day [item]
  (-> (:date item) (* 1000) (c/from-long) (t/day)))

;; Transducers
(def partition-by-date (partition-by day))
(def average-temperature
  (map (fn [item]
         {:temp (paverage-by :temperature item)
          :date (-> item first :date (* 1000))})))

(defn datetime->string [datetime]
  (f/unparse datetime-format datetime))

(defn date->string [date]
  (f/unparse date-format date))

(defn max-by [key coll]
  {:pre [(seq coll)
         (keyword? key)]}
  (key (apply max-key key coll)))

(defn min-by [key coll]
  {:pre [(seq coll)
         (keyword? key)]}
  (key (apply min-key key coll)))

(defn average-by [key coll]
  {:pre [(seq coll)
         (keyword? key)]}
  (let [sum (reduce #(+ (key %2) %1) 0 coll)]
    (math/round (/ sum (count coll)))))

(defn paverage-by [key coll]
  {:pre [(seq coll)
         (keyword? key)]}
  (let [sum (r/fold + (r/map key coll))]
    (math/round (/ sum (count coll)))))

(defn dedupe-by [pred]
  (fn [xf]
    (let [prev (volatile! ::none)]
      (fn
        ([] (xf))
        ([result] (xf result))
        ([result input]
          (let [prior @prev]
            (vreset! prev (pred input))
            (if (= prior (pred input))
              result
              (xf result input))))))))

(defn average-temperature-by-day [coll]
  (let [avg-by-day (comp partition-by-date average-temperature)]
    (into [] avg-by-day coll)))

;; (defn temperature-by-hour [coll]
;;   (map (fn [item]
;;          {:temp (:temperature item)
;;           :date (* (:date item) 1000)}) coll))

(defn temperature-by-hour [coll]
  (let [temp-map (map (fn [item]
                        {:temp (:temperature item)
                         :date (* (:date item) 1000)}))
        xf (comp temp-map (dedupe-by :date))]
    (into [] xf coll)))

(defn- filter-dates-before [year month day date]
  (let [limit (t/date-time year month day)
        current (c/from-long (:date date))]
    (t/before? limit current)))

(defn- valid-spring-dates [year]
  (partial filter-dates-before year 2 14))

(defn- valid-autumn-dates [year]
  (partial filter-dates-before year 8 1))

(defn season-dates [coll year]
  (let [spring-candidates (valid-spring-dates year)
        autumn-candidates (valid-autumn-dates year)
        temps (average-temperature-by-day coll)
        spring-pred (fn [xs]
                      (filter #(<= 0 (:temp %)) xs))
        summer-pred (fn [xs]
                      (filter #(<= 10 (:temp %)) xs))
        autumn-pred (fn [xs]
                      (filter #(>= 10 (:temp %)) xs))
        winter-pred (fn [xs]
                      (filter #(>= 0 (:temp %)) xs))]
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

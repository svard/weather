(ns weather.utils.date
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.local :as l]))

(def days-in-month [31 28 31 30 31 30 31 31 30 31 30 31])

(defn get-days-of-month [month]
  (nth days-in-month (dec month)))

(defn get-month-start-end-timestamp [year month]
  (let [start (t/date-time year month 1)
        end (t/date-time year month (get-days-of-month month) 23 59 59)]
    [(/ (c/to-long start) 1000) (/ (c/to-long end) 1000)]))

(defn get-year-start-end-timestamp [year]
  (let [start (t/date-time year 1 1)
        end (t/date-time year 12 31 23 59 59)]
    [(/ (c/to-long start) 1000) (/ (c/to-long end) 1000)]))

(defn get-date-start-end-timestamp [xs ys]
  (let [start (apply t/date-time xs)
        end (apply t/date-time (concat ys [23 59 59]))]
    [(/ (c/to-long start) 1000) (/ (c/to-long end) 1000)]))

(defn today []
  (let [now (l/local-now)]
    [(t/year now) (t/month now) (t/day now)]))

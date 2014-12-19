(ns weather.database.db
  (:require [monger.collection :as coll :exclude [update]]
            [monger.query :as query]
            [monger.joda-time]))

(defn make-temp-query [db]
  (fn [[start end]]
    (->> (query/with-collection db "temperatures"
           (query/find {:date {"$lt" end, "$gte" start}})
           (query/fields [:temperature :dateStr :date :deviceId :deviceName]))
      (map #(update-in % [:_id] str)))))

(defn current-humidity [db]
  (query/with-collection db "rainrates"
    (query/find {})
    (query/sort {:date -1})
    (query/fields [:date :humidity])
    (query/limit 1)))

(defn current-precipitation [db]
  (query/with-collection db "rainrates"
    (query/find {})
    (query/sort {:date -1})
    (query/fields [:date :rainrate])
    (query/limit 1)))

(defn aggregated-precipitation
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

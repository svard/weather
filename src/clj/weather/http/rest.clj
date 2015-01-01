(ns weather.http.rest
  (:require [liberator.core :refer [defresource]]
            [monger.core :as mg]
            [clj-time.core :as t]
            [clj-time.local :as l]
            [taoensso.timbre :as timbre]
            [taoensso.carmine :as car :refer (wcar)]
            [weather.http.helpers :as h]
            [weather.database.db :as d]
            [weather.utils.math :as math]
            [weather.utils.date :as date]))

(timbre/refer-timbre)

(def conn (mg/connect {:host "192.168.0.108" :port 27017}))
(def db (mg/get-db conn "services_db"))

(defn- date-range [query-params]
  (let [year (read-string (:year query-params))
        month (read-string (:month query-params "0"))]
    (if (= month 0)
      (date/get-year-start-end-timestamp year)
      (date/get-month-start-end-timestamp year month))))

(defresource month-temp [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [query-fn (d/make-temp-query db)
                   entity (query-fn (date-range params))]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok (fn [ctx]
               {:avg (h/average-by :temperature (:entity ctx))
                :max (h/max-by :temperature (:entity ctx))
                :min (h/min-by :temperature (:entity ctx))}))

(defresource precipitation [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [year (read-string (:year params))
                   month (read-string (:month params))
                   [_ _ day] (date/today)
                   monthly (first (d/aggregated-precipitation db year month))
                   daily (first (d/aggregated-precipitation db year month day))
                   current (first (d/current-precipitation db))]
               (when (and (seq monthly) (seq daily))
                 {:entity {:daily daily
                           :monthly monthly
                           :current current}})))
  :handle-ok (fn [ctx]
               (let [entity (:entity ctx)]
                 {:daily (math/round (:sum (:daily entity)))
                  :monthly (math/round (:sum (:monthly entity)))
                  :date (h/datetime->string (get-in entity [:current :date]))})))

(defresource humidity [db req]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [entity (first (d/current-humidity db))]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok (fn [ctx]
               (let [entity (:entity ctx)]
                 {:humidity (:humidity entity)
                  :date (h/datetime->string (:date entity))})))

(defresource seasons [db req]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [query-fn (d/make-temp-query db)
                   current-year (t/year (l/local-now))
                   entity (for [year (range 2013 (inc current-year))]
                            (-> (date/get-year-start-end-timestamp year)
                              (query-fn)
                              (h/season-dates year)))]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok (fn [ctx]
               (into [] (:entity ctx))))

(defresource line-data [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [query-fn (d/make-temp-query db)
                   entity (query-fn (date-range params))]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok (fn [ctx]
               (case (:type params)
                 "hourly" (h/temperature-by-hour (:entity ctx))
                 "daily" (h/average-temperature-by-day (:entity ctx))
                 :else [])))

(defresource bar-data [db {:keys [params] :as req}]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (let [year (read-string (:year params))
                   entity (d/aggregated-precipitation db year)]
               (when (seq entity)
                 {:entity entity})))
  :handle-ok (fn [ctx]
               (let [year (read-string (:year params))]
                 (into [] (map (fn [itm]
                                 (let [[ts _] (date/get-month-start-end-timestamp year (:_id itm))]
                                   {:date (* ts 1000)
                                    :precipitation (math/round (:sum itm))}))
                               (:entity ctx))))))

;; (defresource chart-data [db {:keys [params] :as req}]
;;   :available-media-types ["application/edn"]
;;   :exists? (fn [_]
;;              (let [year (read-string (:year params))
;;                    entity (d/aggregated-precipitation db year)]
;;                (when (seq entity)
;;                  {:entity entity})))
;;   :handle-ok (fn [ctx]
;;                (let [year (read-string (:year params))]
;;                  (into [] (map (fn [itm]
;;                                  (let [[ts _] (date/get-month-start-end-timestamp year (:_id itm))]
;;                                    {:date (* ts 1000)
;;                                     :precipitation (math/round (:sum itm))}))
;;                                (:entity ctx))))))


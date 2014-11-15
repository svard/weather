(ns weather.xhr
  (:require [goog.events :as events]
            [cljs.reader :as reader])
  (:import [goog.net EventType XhrIo]))

(def ^:private http-methods
  {:get "GET"
   :post "POST"
   :put "PUT"
   :delete "DELETE"})

(defn edn-xhr [{:keys [method url data on-success on-error]}]
  (let [xhr (XhrIo.)]
    (events/listen xhr
                   EventType.SUCCESS
                   (fn [e]
                     (on-success (reader/read-string (.getResponseText xhr)))))

    (events/listen xhr
                   EventType.ERROR
                   (fn [e]
                     (on-error {:code (.getStatus xhr) :error (reader/read-string (.getResponseText xhr))})))

    (. xhr
       (send url (http-methods method) (when data (pr-str data)) #js {"Content-Type" "application/edn" "Accept" "application/edn"}))))


(ns weather.state
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [<! put!]]
            [weather.xhr :as xhr]
            [weather.channels :as ch]))

(defonce app (atom {}))

(defn initial-state []
  {:indoor {:current nil}
   :outdoor {:current nil}
   :month-temp {:current nil
                :previous nil
                :line []}
   :temp-chart []
   :precipitation {:humidity nil}
   :seasons []})

(defn live-temps [type]
  (case type
    "indoor" (om/ref-cursor (:indoor (om/root-cursor app)))
    "outdoor" (om/ref-cursor (:outdoor (om/root-cursor app)))))

(defn month-temps []
  (om/ref-cursor (:month-temp (om/root-cursor app))))

(defn precipitation []
  (om/ref-cursor (:precipitation (om/root-cursor app))))

(defn seasons []
  (om/ref-cursor (:seasons (om/root-cursor app))))

(defn temp-chart []
  (om/ref-cursor (:temp-chart (om/root-cursor app))))

(go-loop []
  (let [[type year month] (<! ch/month-temp-chan)]
    (xhr/edn-xhr {:method :get
                  :url (str "temperature/stats?year=" year "&month=" month)
                  :on-success (fn [data]
                                (put! ch/month-resp-chan :done)
                                (swap! app update-in [:month-temp type] (fn [_] data)))
                  :on-error (fn [_]
                              (put! ch/month-resp-chan :done)
                              (swap! app update-in [:month-temp type] (fn [_] {:min "--"
                                                                               :max "--"
                                                                               :avg "--"})))})
    (recur)))

(go-loop []
  (let [[year month] (<! ch/prep-chan)]
    (xhr/edn-xhr {:method :get
                  :url (str "precipitation?year=" year "&month=" month)
                  :on-success (fn [data]
                                (put! ch/prep-resp-chan :done)
                                (swap! app update-in [:precipitation] (fn [_] data)))
                  :on-error (fn [_]
                              (put! ch/prep-resp-chan :done)
                              (prn "Failed to fetch precipitation"))})
    (recur)))

(go-loop []
  (let [[year month] (<! ch/month-line-chan)]
    (xhr/edn-xhr {:method :get
                  :url (str "temperature/line?year=" year "&month=" month)
                  :on-success (fn [data]
                                (swap! app update-in [:temp-chart] (fn [_]
                                                                     (vec data))))
                  :on-error (fn [_]
                              (swap! app update-in [:temp-chart] (fn [_] [])))})
    (recur)))

(go-loop []
  (<! ch/season-chan)
  (xhr/edn-xhr {:method :get
                :url "seasons"
                :on-success (fn [data]
                              (put! ch/season-resp-chan :done)
                              (swap! app update-in [:seasons] (fn [_] data)))
                :on-error (fn [_]
                            (put! ch/season-resp-chan :done)
                            (prn "Failed to fetch seasons"))})
  (recur))

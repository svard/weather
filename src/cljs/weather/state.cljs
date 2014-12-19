(ns weather.state
  (:require [om.core :as om :include-macros true]
            [weather.xhr :as xhr]))

(defonce app (atom {}))

(defn initial-state []
  {:indoor {}
   :outdoor {}
   :month-temp {}
   :temp-chart []
   :precipitation {}
   :humidity {}
   :seasons []})

(defn live-temps [type]
  (case type
    "indoor" (om/ref-cursor (:indoor (om/root-cursor app)))
    "outdoor" (om/ref-cursor (:outdoor (om/root-cursor app)))))

(defn month-temps []
  (om/ref-cursor (:month-temp (om/root-cursor app))))

(defn precipitation []
  (om/ref-cursor (:precipitation (om/root-cursor app))))

(defn humidity []
  (om/ref-cursor (:humidity (om/root-cursor app))))

(defn seasons []
  (om/ref-cursor (:seasons (om/root-cursor app))))

(defn temp-chart []
  (om/ref-cursor (:temp-chart (om/root-cursor app))))

(defn load-data [url success-fn error-fn]
  (xhr/edn-xhr {:method :get
                :url url
                :on-success success-fn
                :on-error error-fn}))

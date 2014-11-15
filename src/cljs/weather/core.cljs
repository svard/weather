(ns weather.core
  (:require [om.core :as om :include-macros true]
            [weather.state :as state]
            [weather.ui :as ui]
            [weather.websocket :as ws]))

(enable-console-print!)

(reset! state/app (state/initial-state))

(om/root ui/temperature-view state/app {:target (. js/document (getElementById "temperature"))})
(om/root ui/precipitation-view state/app {:target (. js/document (getElementById "precipitation"))})
(om/root ui/seasons-view state/app {:target (. js/document (getElementById "seasons"))})
(om/root ui/simple-line-view state/app {:target (. js/document (getElementById "chart"))})

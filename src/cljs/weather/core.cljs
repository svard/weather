(ns weather.core
  (:require [om.core :as om :include-macros true]
            [weather.state :as state]
            [weather.ui :as ui]
            [weather.websocket :as ws]))

(enable-console-print!)

(reset! state/app (state/initial-state))

(om/root ui/temperature-root state/app {:target (. js/document (getElementById "temperature"))})
(om/root ui/precipitation-root state/app {:target (. js/document (getElementById "precipitation"))})
(om/root ui/seasons-root state/app {:target (. js/document (getElementById "seasons"))})
(om/root ui/chart-root state/app {:target (. js/document (getElementById "chart"))})


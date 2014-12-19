(ns weather.channels
  (:require [cljs.core.async :refer [chan]]))

(def live-update-chan
  (chan))

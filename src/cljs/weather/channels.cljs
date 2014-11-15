(ns weather.channels
  (:require [cljs.core.async :refer [chan mult]]))

(def month-temp-chan (chan))
(def month-nav-chan (chan))
(def month-resp-chan (chan))

(def month-line-chan (chan))

(def prep-chan (chan))
(def prep-resp-chan (chan))
(def prep-resp-mult (mult prep-resp-chan))

(def season-chan (chan))
(def season-resp-chan (chan))

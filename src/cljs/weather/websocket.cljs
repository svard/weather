(ns weather.websocket
  (:require-macros [cljs.core.match.macros :refer [match]]
                   [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.match]
            [taoensso.sente :as sente]
            [cljs.core.async :refer [put! <!]]
            [weather.state :as state]
            [weather.channels :as ch]))

(enable-console-print!)

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk chsk)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defn handle-response [type reply]
  (if (sente/cb-success? reply)
    (swap! state/app update-in [type] (fn [_] reply))
    (prn "Unknown response from server" reply)))

(defn event-handler [{:keys [event]}]
  (match event
         [:chsk/state {:first-open? true}]
         (do
           (chsk-send! [:live-temp/init :indoor] 3000 (partial handle-response :indoor))
           (chsk-send! [:live-temp/init :outdoor] 3000 (partial handle-response :outdoor)))
         [:chsk/recv payload]
         (let [[msg-type msg] payload]
           (match [msg-type msg]
                  [:live-temp/indoor data] (swap! state/app update-in [:indoor] (fn [_] data))
                  [:live-temp/outdoor data] (swap! state/app update-in [:outdoor] (fn [_] data))))))

(defonce event-router
  (sente/start-chsk-router! ch-chsk event-handler))

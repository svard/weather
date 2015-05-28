(ns weather.websocket.component
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [taoensso.sente :as sente]
            [weather.websocket.websocket :as ws]))

(timbre/refer-timbre)

(defrecord KeyStore [conf]
  component/Lifecycle
  (start [component]
    (info "Starting key store")
    (assoc component :store (:key-store conf)))

  (stop [component]
    (info "Stopping key store")
    (assoc component :store nil)))

(defn new-key-store [conf]
  (map->KeyStore {:conf conf}))

(defrecord Websocket [channels store]
  component/Lifecycle
  (start [component]
    (info "Starting websocket listener")    
    (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
          (sente/make-channel-socket! {:user-id-fn ws/user-id})
          chsk-router (ws/make-event-handler (:store store))]
      (ws/start-loop (:mb-comm channels) (:store store) (ws/live-stats send-fn connected-uids))
      (sente/start-chsk-router! ch-recv chsk-router)
      (assoc component
        :ajax-post-fn ajax-post-fn
        :ajax-get-or-ws-handshake-fn ajax-get-or-ws-handshake-fn
        :ch-chsk ch-recv
        :chsk-send! send-fn
        :connected-uids connected-uids)))

  (stop [component]
    (assoc component
      :ajax-post-fn nil
      :ajax-get-or-ws-handshake-fn nil
      :ch-chsk nil
      :chsk-send! nil
      :connected-uids nil)))

(defn new-websocket []
  (map->Websocket {}))

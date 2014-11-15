(ns weather.websocket.component
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [taoensso.sente :as sente]
            [weather.websocket.websocket :as ws]
            [weather.websocket.redis :as redis]))

(timbre/refer-timbre)

(defrecord RedisStore [conf]
  component/Lifecycle
  (start [component]
    (info "Starting redis store")
    (assoc component :redis (redis/make-transient {:pool {}
                                                   :spec {:host (:redis-host conf)
                                                          :port 6379}}
                                                  :expire 129600)))

  (stop [component]
    (info "Stopping redis store")
    (assoc component :redis nil)))

(defn new-redis-store [conf]
  (map->RedisStore {:conf conf}))

(defrecord Websocket [channels redis]
  component/Lifecycle
  (start [component]
    (info "Starting websocket listener")    
    (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
          (sente/make-channel-socket! {:user-id-fn ws/user-id})
          chsk-router (ws/make-event-handler (:redis redis))]
      (ws/start-loop (:mb-comm channels) (:redis redis) (ws/live-stats send-fn connected-uids))
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

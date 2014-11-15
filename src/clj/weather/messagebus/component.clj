(ns weather.messagebus.component
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan put!]]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [langohr.basic :as lb]))

(timbre/refer-timbre)

(defrecord MessageBus [conf channels]
  component/Lifecycle
  (start [component]
    (info "Connecting message bus on host" (:mb-host conf))
    (let [conn (rmq/connect {:host (:mb-host conf)})
          ch (lch/open conn)
          q (.getQueue (lq/declare ch
                                   (str "weather-queue@" (.. java.net.InetAddress getLocalHost getHostName))
                                   {:exclusive false
                                    :auto-delete true}))]
      (lq/bind ch q (:mb-exchange conf) {:routing-key (:temp-routing-key conf)})
      (lc/subscribe ch q (fn [_ _ ^bytes payload]
                           (let [msg (json/read-str (String. payload "UTF-8") :key-fn keyword)]
                             (put! (:mb-comm channels) msg))) {:auto-ack true})
      (assoc component
        :mb-conn conn
        :mb-ch ch)))

  (stop [component]
    (info "Stopping message bus")
    (rmq/close (:ch-temp component))
    (rmq/close (:mb-ch component))
    (assoc component
      :mb-conn nil
      :mb-ch nil)))

(defn new-messagebus [conf]
  (map->MessageBus {:conf conf}))

(defrecord MessageBusChannels []
  component/Lifecycle
  (start [component]
    (info "Creating channels")
    (assoc component
      :mb-comm (chan)))
  (stop [component]
    (info "Tear down channels")
    (assoc component
      :mb-comm nil)))

(defn new-messagebus-channels []
  (map->MessageBusChannels {}))

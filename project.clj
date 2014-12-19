(defproject weather "1.2.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.5.0"

  :source-paths ["src/clj" "src/cljs"]

  :test-paths ["test/clj" "test/cljs"]
  
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/clojurescript "0.0-2371" :scope "provided"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/core.match "0.2.1"]
                 [ring "1.3.1"]
                 [compojure "1.2.0"]
                 [liberator "0.12.0"]
                 [http-kit "2.1.19"]
                 [om "0.8.0-beta3"]
                 [prismatic/om-tools "0.3.6"]
                 [com.novemberain/monger "2.0.0"]
                 [com.novemberain/langohr "3.0.0-rc3"]
                 [com.taoensso/timbre "3.3.1" :exclusions [com.taoensso/encore]]
                 [com.taoensso/sente "1.2.0"]
                 [com.taoensso/carmine "2.7.1" :exclusions [com.taoensso/encore]]
                 [com.stuartsierra/component "0.2.2"]
                 [sablono "0.2.22"]
                 [clj-time "0.8.0"]]
  
  :plugins [[lein-ring "0.8.12"]
            [lein-cljsbuild "1.0.3"]
            [cider/cider-nrepl "0.8.1"]]

  :main ^:skip-aot weather.core

  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}
             :uberjar {:aot :all}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/weather_dev.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/weather.js"
                                   :optimizations :advanced
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"
                                             "resources/public/bower_components/d3/d3.js"
                                             "resources/public/bower_components/dimple/dist/dimple.v2.1.0.js"]
                                   :pretty-print false
                                   :closure-warnings {:externs-validation :off
                                                      :non-standard-jsdoc :off}}}]})


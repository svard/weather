(ns weather.charts)

(defn nvd3-simple-line [data]
  (let [chart (.. js/nv -models lineChart
                  (margin #js {:left 75})
                  (useInteractiveGuideline true)
                  (transitionDuration 350)
                  (showLegend true)
                  (showYAxis true)
                  (showXAxis true))]
    (.. chart -xAxis (axisLabel "Date") (tickFormat (fn [d]
                                                      (let [f (.. js/d3 -time (format "%Y-%m-%d"))]
                                                        (f (js/Date. d))))))
    (.. chart -yAxis (axisLabel "°C") (tickFormat (.format js/d3 ",r")))
    (.. js/d3 (select "#nv-line svg")
        (datum #js [
                    #js {:values (clj->js data)
                         :key "Temperature"
                         :color "green"
                         }
                    ])
        (call chart))
    (.. js/nv -utils (windowResize (.update chart)))))

(defn nvd3-line-plus-bar [line bar]
  (let [chart (.. js/nv -models linePlusBarChart
                  (margin #js {:left 75
                               :top 75})
                  (x (fn [d i] i))
                  (y (fn [d i] (aget d 1))))]
    (.. chart -xAxis (tickFormat (fn [d]
                                   (let [dx (-> line
                                                (get d)
                                                (get 0))
                                         f (.. js/d3 -time (format "%Y-%m-%d"))]
                                     (f (js/Date. dx))))))
    (.. chart -y1Axis (tickFormat (.format js/d3 ",f")))
    (.. chart -y2Axis (tickFormat (fn [d]
                                    (let [f (.. js/d3 (format ",f"))]
                                      (str "$" (f d))))))
    (.. js/d3 (select "#nv-line svg")
        (datum #js [
                    #js {:values (clj->js line)
                         :key "Sample data"
                         :color "red"
                         }
                    #js {:values (clj->js bar)
                         :bar true
                         :key "Bar data"
                         :color "blue"
                         }
                    ])
        (transition)
        (duration 0)
        (call chart))
    (.. js/nv -utils (windowResize (.update chart)))))

(ns weather.charts)

(defn- format-date [data date-format]
  (let [format (.. js/d3 -time (format date-format))]
    (map (fn [itm]
           {:date (format (js/Date. (:date itm)))
            :precipitation (:precipitation itm)}) data)))

(defn dimple-line [id data {:keys [color date-format interval period]}]
  (when (seq data)
    (let [Chart (.-chart js/dimple)
          svg (.newSvg js/dimple (str "#" id) "100%" 500)
          line (.. (Chart. svg (clj->js data))
                   (setMargins 50 20 50 50))
          x (.addTimeAxis line "x" "date" nil date-format)
          y (.addMeasureAxis line "y" "temp")
          s (.addSeries line nil js/dimple.plot.line)
          Color (.-color js/dimple)]
      (aset x "title" nil)
      (aset x "timePeriod" period)
      (aset x "timeInterval" interval)
      (aset y "title" "Temperature °C")
      (aset y "tickFormat" "g")
      (aset line "defaultColors" #js [(Color. color)])
      (.draw line))))

(defn dimple-bar [id data {:keys [color date-format interval period]}]
  (when (seq data)
    (let [Chart (.-chart js/dimple)
          svg (.newSvg js/dimple (str "#" id) "100%" 500)
          bar (.. (Chart. svg (clj->js (format-date data date-format)))
                  (setMargins 50 20 50 50))
          x (.addCategoryAxis bar "x" "date")
          y (.addMeasureAxis bar "y" "precipitation")
          s (.addSeries bar nil js/dimple.plot.bar)
          Color (.-color js/dimple)
          format (.. js/d3 -time (format date-format))]
      (aset x "title" nil)
      (aset x "timePeriod" period)
      (aset x "timeInterval" interval)
      (aset y "title" "Precipitation mm")
      (aset y "tickFormat" "g")
      (aset bar "defaultColors" #js [(Color. color)])
      (.addOrderRule x (fn [a b]
                         (let [a (.getTime (.. format (parse (get (js->clj a) "date"))))
                               b (.getTime (.. format (parse (get (js->clj b) "date"))))]
                           (cond
                             (> a b) 1
                             (< a b) -1
                             :else 0))))                         
      (.draw bar))))

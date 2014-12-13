(ns weather.charts)

(defn dimple-line [id data]
  (let [Chart (.-chart js/dimple)
        svg (.newSvg js/dimple (str "#" id) "100%" 500)
        line (.. (Chart. svg (clj->js data))
                 (setMargins 50 20 20 50))
        x (.addTimeAxis line "x" "date" nil "%Y-%m-%d %H:%M")
        y (.addMeasureAxis line "y" "temp")
        s (.addSeries line nil js/dimple.plot.line)
        Color (.-color js/dimple)]
    (aset x "title" nil)
    (aset x "timePeriod" js/d3.time.day)
    (aset x "timeInterval" 3)
    (aset y "title" "Temperature")
    (aset y "tickFormat" "g")
    (aset line "defaultColors" #js [(Color. "#66CD00")])
    (.draw line)))

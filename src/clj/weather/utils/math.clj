(ns weather.utils.math)

(defn round [x]
  (double (/ (Math/round (* x 10)) 10)))

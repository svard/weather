(ns weather.date)

(def month-name ["January" "February" "March" "April" "May" "June" "July" "August" "September" "October" "November" "December"])
(def wrap-month (comp inc mod))

(defprotocol IDateFormat
  (-format-date [this])
  (-format-time [this])
  (-format-datetime [this]))

(extend-protocol IDateFormat
  js/Date
  (-format-date [date]
    (let [month (inc (.getMonth date))
          day (.getDate date)]
      (str (.getFullYear date) "-" (if (< month 10) (str "0" month) month) "-" (if (< day 10) (str "0" day) day))))
  (-format-time [date]
    (let [hours (.getHours date)
          minutes (.getMinutes date)
          seconds (.getSeconds date)]
      (str (if (< hours 10) (str "0" hours) hours) ":" (if (< minutes 10) (str "0" minutes) minutes) ":" (if (< seconds 10) (str "0" seconds) seconds))))
  (-format-datetime [date]
    (str (-format-date date) " " (-format-time date))))

(defn format-date [date]
  (cond
    (or (goog/isString date) (goog/isNumber date))
    (let [d (js/Date. date)]
      (when-not (js/isNaN d)
        (-format-date d)))

    (satisfies? IDateFormat date)
    (-format-date date)))

(defn format-time [date]
  (cond
    (or (goog/isString date) (goog/isNumber date))
    (let [d (js/Date. date)]
      (when-not (js/isNaN d)
        (-format-time d)))

    (satisfies? IDateFormat date)
    (-format-time date)))

(defn format-datetime [date]
  (cond
    (or (goog/isString date) (goog/isNumber date))
    (let [d (js/Date. date)]
      (when-not (js/isNaN d)
        (-format-datetime d)))

    (satisfies? IDateFormat date)
    (-format-datetime date)))

(defn get-month-name
  [month]
  (nth month-name (dec month)))

(defn get-year
  []
  (.getFullYear (js/Date.)))

(defn get-month
  []
  (inc (.getMonth (js/Date.))))

(defn next-month [month]
  (wrap-month month 12))

(defn prev-month [month]
  (wrap-month (- month 2) 12))


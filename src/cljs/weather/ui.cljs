(ns weather.ui
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clojure.string :as string]
            [cljs.core.async :refer [put! tap chan pub sub <!]]
            [weather.state :as state]
            [weather.date :as date]
            [weather.xhr :as xhr]
            [weather.channels :as ch]
            [weather.charts :as chart]
            [weather.widgets.select-box :refer [select-box]]
            ))

(defn on-prev-click [owner year month]
  (let [new-month (date/prev-month month)
        new-year (if (> new-month month)
                   (dec year)
                   year)]
    (om/set-state! owner :month new-month)
    (om/set-state! owner :year new-year)
    (put! ch/month-temp-chan [:current new-year new-month])
    (put! ch/month-temp-chan [:previous (dec new-year) new-month])
    (om/set-state! owner :loading true)))

(defn on-next-click [owner year month]
  (let [new-month (date/next-month month)
        new-year (if (< new-month month)
                   (inc year)
                   year)]
    (om/set-state! owner :month new-month)
    (om/set-state! owner :year new-year)
    (put! ch/month-temp-chan [:current new-year new-month])
    (put! ch/month-temp-chan [:previous (dec new-year) new-month])
    (om/set-state! owner :loading true)))

(defn live-panel [{:keys [type]} owner]
  (reify
    om/IRender
    (render [_]
      (let [xs (om/observe owner (state/live-temps type))]
        (html [:div.uk-width-large-1-2.uk-width-small-1-1.uk-margin-large-bottom
               [:div.uk-panel.uk-panel-box
                [:div.uk-panel-badge.uk-badge "Live"]
                [:h3.uk-panel-title (string/capitalize type)]
                [:div.w-large.uk-text-bold.uk-text-right (str (:current xs) "°C")]
                [:div.uk-text-right.w-sub-info
                 [:span.w-cold (str (:low xs) "°")]
                 [:span (str (:avg xs) "°")]
                 [:span.w-warm (str (:high xs) "°")]]
                [:div.uk-text-small.uk-text-muted (str "Last updated " (date/format-datetime (:date xs)))]]])))))

(defn month-table [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:year (.getFullYear (js/Date.))
       :month (inc (.getMonth (js/Date.)))
       :loading true})
    om/IWillMount
    (will-mount [_]
      (let [year (om/get-state owner :year)
            month (om/get-state owner :month)]
        (put! ch/month-temp-chan [:current year month])
        (put! ch/month-temp-chan [:previous (dec year) month]))

      (go-loop []
        (<! ch/month-resp-chan)
        (om/set-state! owner :loading false)
        (recur)))
    om/IRenderState
    (render-state [_ {:keys [year month loading]}]
      (let [xs (om/observe owner (state/month-temps))]
        (html [:div.uk-width-1-1.w-holder
               [:h3 (date/get-month-name month)]
               [:i.w-spinner.uk-icon-spinner.uk-icon-spin.uk-icon-large {:style (if loading
                                                                                   #js {:display "block"}
                                                                                   #js {:display "none"})}]
               [:table.uk-table.uk-table-striped {:style (if loading
                                                           #js {:visibility "hidden"}
                                                           #js {:visibility "visible"})}
                [:thead
                 [:tr
                  [:th]
                  [:th "Low"]
                  [:th "Average"]
                  [:th "High"]]]
                [:tbody
                 [:tr
                  [:td.uk-text-bold year]
                  [:td.w-cold (str (get-in xs [:current :min]) "°C")]
                  [:td (str (get-in xs [:current :avg]) "°C")]
                  [:td.w-warm (str (get-in xs [:current :max]) "°C")]]
                 [:tr
                  [:td.uk-text-bold (dec year)]
                  [:td.w-cold (str (get-in xs [:previous :min]) "°C")]
                  [:td (str (get-in xs [:previous :avg]) "°C")]
                  [:td.w-warm (str (get-in xs [:previous :max]) "°C")]]]]
               [:ul.uk-pagination
                [:li.uk-pagination-previous.uk-active
                 [:a {:on-click #(on-prev-click owner year month)}
                  [:i.uk-icon-angle-double-left]
                  " Prev"]]
                [:li.uk-pagination-next
                 [:a {:on-click #(on-next-click owner year month)}
                  "Next "
                  [:i.uk-icon-angle-double-right]]]]])))))

(defn humidity-panel [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:loading true})
    om/IWillMount
    (will-mount [_]
      (let [loaded? (tap ch/prep-resp-mult (chan))]
        (go-loop []
          (<! loaded?)
          (om/set-state! owner :loading false))))
    om/IRenderState
    (render-state [_ {:keys [loading]}]
      (let [xs (om/observe owner (state/precipitation))]
        (html [:div.uk-width-large-1-2.uk-width-small-1-1.uk-margin-bottom
               [:div.uk-panel.uk-panel-box
                [:i.w-spinner.uk-icon-spinner.uk-icon-spin.uk-icon-large {:style (if loading
                                                                                   #js {:display "block"}
                                                                                   #js {:display "none"})}]
                [:div {:style (if loading
                                #js {:visibility "hidden"}
                                #js {:visibility "visible"})}
                 [:h3.uk-panel-title "Humidity"]
                 [:div.w-large.uk-text-bold.uk-text-right.uk-margin-bottom (str (:humidity xs) "%")]
                 [:div.uk-text-small.uk-text-muted (str "Last updated " (date/format-datetime (:date xs)))]]]])))))

(defn precipitation-panel [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:year (.getFullYear (js/Date.))
       :month (inc (.getMonth (js/Date.)))
       :loading true})
    om/IWillMount
    (will-mount [_]
      (let [year (om/get-state owner :year)
            month (om/get-state owner :month)
            loaded? (tap ch/prep-resp-mult (chan))]
        (put! ch/prep-chan [year month])        
        (go-loop []
          (<! loaded?)
          (om/set-state! owner :loading false))))
    om/IRenderState
    (render-state [_ {:keys [month loading]}]
      (let [xs (om/observe owner (state/precipitation))]
        (html [:div.uk-width-large-1-2.uk-width-small-1-1.uk-margin-bottom
               [:div.uk-panel.uk-panel-box
                [:i.w-spinner.uk-icon-spinner.uk-icon-spin.uk-icon-large {:style (if loading
                                                                                   #js {:display "block"}
                                                                                   #js {:display "none"})}]
                [:div {:style (if loading
                                #js {:visibility "hidden"}
                                #js {:visibility "visible"})}
                 [:h3.uk-panel-title "Precipitation"]
                 [:div.w-large.uk-text-bold.uk-text-right (str (:current xs) "mm")]
                 [:div.w-sub-info.uk-text-right (str (date/get-month-name month) " " (:month xs) "mm")]
                 [:div.uk-text-small.uk-text-muted (str "Last updated " (date/format-datetime (:date xs)))]]]])))))

(defn temperature-view [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:div.uk-grid
             (om/build live-panel {:type "indoor"})
             (om/build live-panel {:type "outdoor"})
             (om/build month-table app)
             ]))))

(defn precipitation-view [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:div.uk-grid
             (om/build precipitation-panel app)
             (om/build humidity-panel app)]))))

(defn chart-view [_ owner]
  (reify
    om/IInitState
    (init-state [_]
      {:year (.getFullYear (js/Date.))
       :month (inc (.getMonth (js/Date.)))
       :month-select-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [year (om/get-state owner :year)
            month (om/get-state owner :month)
            select-chan (om/get-state owner :month-select-chan)]
        (put! ch/month-line-chan [year month])
        (go-loop []
          (let [[_ month] (<! select-chan)]
            (put! ch/month-line-chan [year (:value month)]))
          (recur))))
    om/IDidUpdate
    (did-update [_ _ _]
      (let [data (om/observe owner (state/temp-chart))
            el (.getElementById js/document "line-chart")]
        (while (.hasChildNodes el)
          (.removeChild el (.-lastChild el)))
        (when (seq data)
          (chart/dimple-line "line-chart" data))))
    om/IRenderState
    (render-state [_ {:keys [year month month-select-chan]}]
      (let [months (map-indexed (fn [idx itm]
                                  {:title itm :value (inc idx)}) date/month-name)]
        (html [:div.uk-grid
               [:div.uk-align-right (om/build select-box months {:opts {:default (date/get-month-name month)
                                                                        :chan month-select-chan}})]
               [:div#line-chart.uk-width-1-1.uk-margin-bottom {:reactKey "line-chart"}]])))))

(defn seasons-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:loading true})
    om/IWillMount
    (will-mount [_]
      (put! ch/season-chan :fetch)
      (go-loop []
        (<! ch/season-resp-chan)
        (om/set-state! owner :loading false)))
    om/IRenderState
    (render-state [_ {:keys [loading]}]
      (let [xs (om/observe owner (state/seasons))]
        (html [:div.w-holder
               [:i.w-spinner.uk-icon-spinner.uk-icon-spin.uk-icon-large {:style (if loading
                                                                                   #js {:display "block"}
                                                                                   #js {:display "none"})}]
               [:table.uk-table.uk-table-striped {:style (if loading
                                                           #js {:visibility "hidden"}
                                                           #js {:visibility "visible"})}
                [:thead
                 [:tr
                  [:th]
                  [:th "Spring"]
                  [:th "Summer"]
                  [:th "Autumn"]
                  [:th "Winter"]]]
                [:tbody
                 (for [x xs]
                   [:tr
                    [:td.uk-text-bold (:year x)]
                    [:td (if (nil? (:spring x)) "N/A" (:spring x))]
                    [:td (if (nil? (:summer x)) "N/A" (:summer x))]
                    [:td (if (nil? (:autumn x)) "N/A" (:autumn x))]
                    [:td (if (nil? (:winter x)) "N/A" (:winter x))]])]]])))))

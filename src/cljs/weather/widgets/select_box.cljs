(ns weather.widgets.select-box
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :refer [put!]]
            [sablono.core :as html :refer-macros [html]]))

(defn ^:private toggle-active [owner active?]
  (om/set-state! owner :active? (not active?)))

(defn ^:private close-active [owner]
  (om/set-state! owner :active? false))

(defn ^:private select [owner evt chan]
  (let [selected (.. evt -target -text)
        active? (om/get-state owner :active?)
        items (om/get-state owner :items)]
    (toggle-active owner active?)
    (om/set-state! owner :selected selected)
    (put! chan [:select (first (filter #(= selected (:title %)) items))])))

(defn ^:private display [show]
  (if show
    #js {:display "block"}
    #js {:display "none"}))

(defcomponent select-box [items owner {:keys [default chan]}]
  (init-state [_]
              {:active? false
               :selected default
               :items items})
  (render-state [_ {:keys [active? selected]}]
                (html [:div.uk-button-dropdown
                       [:button.w-select-box.uk-button {:style #js {:minWidth "150px"}
                                                        :on-click #(toggle-active owner active?)}
                        (str selected " ")
                        [:i.w-select-box-caret.uk-icon-caret-down]]
                       [:div.uk-dropdown.uk-dropdown-small {:style (display active?)}
                        [:ul.uk-nav.uk-nav-dropdown {:on-click #(select owner %1 chan)}
                         (for [item items]
                           [:li
                            [:a (:title item)]])]]])))

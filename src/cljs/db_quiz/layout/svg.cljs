(ns db-quiz.layout.svg
  (:require [db-quiz.config :refer [config]]
            [db-quiz.logic :as logic]
            [db-quiz.state :refer [app-state]]
            [db-quiz.util :refer [join-by-space shade-colour]]
            [clojure.string :as string]))

(defn get-gradients
  "Generate SVG gradients for given status and colour."
  [status colour]
  (let [{{:keys [hex-shade]} :layout} config
        start [:stop {:offset "0%" :stop-color colour}]
        end [:stop {:offset "100%" :stop-color (shade-colour colour hex-shade)}]
        [inner-id outer-id] (map (partial str status) ["-inner" "-outer"])]
    [^{:key inner-id}
     [:linearGradient {:id inner-id :x1 0 :x2 0 :y1 1 :y2 0} start end]
     ^{:key outer-id}
     [:linearGradient {:id outer-id :x1 0 :x2 0 :y1 0 :y2 1} start end]]))

(defn hex-corner
  "Generate coordinates for a hexagon's corner, where
  [x y] are the coordinates of the hexagon's center,
  size is the hexagon's diameter, and i the corner's degree (from 0 to 5)."
  [[x y] size i]
  (let [round (fn [n] (.toFixed n 5))
        angle-deg (+ (* 60 i) 90)
        angle-rad (* (/ (.-PI js/Math) 180) angle-deg)]
    [(round (+ x (* (/ size 2) (.cos js/Math angle-rad))))
     (round (+ y (* (/ size 2) (.sin js/Math angle-rad))))]))

(defn hex-coords
  "Generates coordinates for a hexagon of size
  centered at center [x y]."
  [center size]
  (string/join " "
               (map (comp (partial string/join ",")
                          (partial hex-corner center size))
                    (range 6))))

(defn hexagon
  "Generate hexagon of size containing text
  centered at center [x y]."
  [{:keys [center id size text]}]
  (let [absolute-offset (* (/ size 100) (get-in config [:layout :inner-hex-offset]))
        [x y] center]
    (fn []
      (let [{:keys [board current-field loading?]} @app-state
            ownership (name (get-in board [id :ownership]))
            disabled? (not (nil? current-field))
            availability (if (or loading? disabled? (not (#{"default" "missed"} ownership)))
                             "unavailable"
                             (case ownership
                                   "default" "available"
                                   "missed" "missed"))]
        [:g {:class (join-by-space "hexagon" availability (when (= ownership "active") "active"))
             :on-click (partial logic/pick-field id)} 
         [:polygon.hex-outer {:fill (str "url(#" ownership "-outer)")
                              :points (hex-coords center size)}]
         [:polygon.hex-inner {:fill (str "url(#" ownership "-inner)")
                              :points (hex-coords (map #(- % absolute-offset) center)
                                                  (- size (* 2 absolute-offset)))
                              :transform (str "translate(" absolute-offset "," absolute-offset ")")}]
         [:text {:x x 
                 :y (+ y (/ size 5)) 
                 :font-size (/ size 2)
                 :text-anchor "middle"}
          text]]))))

(def hex-triangle
  "Component that generates triangular board of hexagons."
  (let [{{:keys [border-width space]
          r :hex-radius} :layout
         n :board-size} config
        size (* 2 r)
        y-space (* size (/ space 100))
        x-space (* y-space (/ (.sqrt js/Math 3) 2))
        w (* (.sqrt js/Math 3) r)
        grid-width (+ (* n w)
                      (* 2 border-width) ; Account for hexagon's border
                      (* (dec n) x-space))
        grid-height (+ (* (/ 3 2) r n) (/ r 2)
                       (* (dec n) y-space) ; Add height for spaces
                       (* 2 border-width)) ; Account for hexagon's border
        x-offset (fn [x y] (+ (* (- n (inc y)) (/ w 2))
                              (* x w)
                              (when-not (= x 1) (* (dec x) x-space)) ; Account for spaces
                              (when-not (= y n) (* (- n y) (/ x-space 2)))
                              border-width)) ; Account for hexagon's border
        y-offset (fn [y] (+ r
                            border-width ; Account for hexagon's border
                            (* (/ 3 2) r (dec y))
                            (when-not (= y 1) (* (dec y) y-space))))] ; Account for spaces
    (fn []
      [:svg#hex-triangle {:x 0
                          :y 0
                          :width grid-width
                          :height grid-height}
       [:defs (mapcat (fn [[status colour]]
                        (get-gradients (name status) colour))
                      (:colours config))]
       (map (fn [[[x y] options]]
              ^{:key [x y]}
              [hexagon (assoc options
                              :center [(x-offset x y)
                                       (y-offset y)]
                              :id [x y]
                              :size size)])
          (:board @app-state))])))

(defn winners-cup
  "Winner's cup coloured with the colour of the winning player."
  [colour]
  [:svg {:width 250 :height 250}
    [:defs
     [:linearGradient
       {:id "cup-gradient"
        :x1 0 :y1 0
        :x2 177.79153 :y2 96.346634
        :gradientUnits "userSpaceOnUse"}
       [:stop {:offset 0
              :style {:stop-color colour
                      :stop-opacity 0}}]
       [:stop {:offset 1
              :style {:stop-color colour
                      :stop-opacity 1}}]]]
   [:g 
    [:path {:style {:fill "url(#cup-gradient)"}
            :d "m 23.094464,-2e-5 23.522065,57.01846 -12.801831,22.16903 45.725784,79.20482 8.89883,0 9.488616,23.019 15.351792,0 0,35.95961 0.0694,0 -33.773938,16.3058 0.50306,16.3233 90.688298,0 0.15613,-16.3233 -33.75659,-16.3058 0.0693,0 0,-35.95961 15.21301,0 9.50598,-23.019 9.03761,0 L 216.70041,79.18747 203.88122,56.9664 227.42063,-2e-5 l -56.42865,0 -91.451568,0 -56.446015,0 z m 175.981796,68.58867 6.12336,10.59882 -36.9657,64.04383 30.84234,-74.64265 z m -147.654708,0.0694 30.44338,73.81003 -36.532055,-63.28059 6.088675,-10.52944 z"
            :id "cup"}]
    [:path {:style {:fill "#ffffff"}
            :d "M 34.526818,-51.55751 52.996969,21.61337 -3.3828659,-29.67722 47.907724,26.68531 -25.263158,8.21516 47.336482,28.79717 -25.263158,51.97574 49.223311,35.73862 -3.3828669,89.86811 50.746623,37.26193 34.526818,111.7484 57.688076,39.14876 78.270092,111.7484 59.79994,38.56021 116.17977,89.86811 64.889185,33.48828 138.04275,51.97574 65.443117,31.39373 138.04275,8.21516 63.573598,24.43496 116.17977,-29.67722 62.050286,22.91165 78.270092,-51.55751 55.091523,21.04213 34.526818,-51.55751 z"
            :id "shine"}]]])
(ns chinstrap.views.pages
  (:require [chinstrap.views.common :as template]
            [noir.response :as nr]
            [clojure.string :as string]
            [chinstrap.models.sqlqueries :as cq])
  (:use [noir.core]
        [chinstrap.db]
        [chinstrap.models.ajax-endpoints]
        [hiccup.element]))

(defpage "/de-analytics/" []
  (render "/de-analytics/info"))

(defpage "/de-analytics/info" []
  (template/info-page
    [:h3 "Discovery Environment App Info by Day"]
    [:br]
    [:div#inner "Pick a date to begin."]
    [:br]
    [:input#date.left {:onChange "getInfo()"}]))

;Page listing the count of different states of Discovery Environment Apps.
(defpage "/de-analytics/apps" []
  (template/apps-page
    [:h3 "Discovery Environment App Status"]
    [:br]
    [:div#inner
      [:h3.left "Running Apps:" [:span#running.right]]
      [:h3.left "Submitted Apps:" [:span#submitted.right]]
      [:h3.left "Failed Apps:" [:span#failed.right]]
      [:h3.left "Completed Apps:" [:span#completed.right]]]
    [:br]
    [:div.collapsibleContainer
      {:title "Currently Running Apps"}
      [:div#running-apps]]
    [:br]
    [:div.collapsibleContainer
      {:title "Submitted Apps"}
      [:div#submitted-apps]]
    [:br]
    [:div.collapsibleContainer
      {:title "Failed Apps"}
      [:div#failed-apps]]))

;Page listing count and info of Components with no transformation activities.
(defpage "/de-analytics/components" []
  (template/components-page
    [:h3 "Discovery Environment Component Info"]
    [:br]
    [:div#inner
      [:h3.left "Used Components:" [:span#with.right]]
      [:h3.left "Unused Components" [:span#without.right]]
      [:h3.left "Total Components:" [:span#all.right]]]
    [:br]
    [:div.collapsibleContainer {:title "Unused Componenent Details"}
      [:button
        {:onClick "$('#unused').table2CSV({header:['#','App Name','Version','Integrator']});"}
        "Export to CSV"]
      [:table#unused
        [:thead
          [:tr [:th ""]
               [:th "Name"]
               [:th "Version"]
               [:th "Integrator"]]]
        [:tbody
          (let [list (cq/unused-app-list) count (count list)]
            (for
              [i (range 0 count) :let [record (nth list i)]]
              [:tr.row
                [:td.rank.center (inc i)]
                [:td.component {:title (:name record)}(:name record)]
                [:td.version.center (if
                  (or (nil? (:version record))
                      (string/blank? (:version record)))
                  "No Version" (:version record))]
                [:td.integrator {:title (:integrator_name record)}
                  (if
                    (or (= "No name" (:integrator_name record))
                        (= "executable" (:integrator_name record)))
                    (:integrator_name record)
                    [:a {:href (str "mailto://" (:email record))}
                    (:integrator_name record)])]]))]]]))

;Page listing information about Integrators.
(defpage "/de-analytics/integrators" []
  (template/integrators-page
    [:h3 "Discovery Environment Integrator Information"]
    [:br]
    [:div#inner[:table#app-info]]
    [:br]
    [:select#choose.chzn-select {:data-placeholder "Select an Integrator"}
      [:option "General Data"]
      (map #(str "<option value='"(:id %)"'>" (:name %) "</option>")(cq/integrator-list))]
    [:br][:br]
    [:div.collapsibleContainer {:title "Discovery Enviroment App Leaderboard"}
      [:button
        {:onClick "$('#leaderboard').table2CSV({header:['#','Contributor Name','Count of Integrated Apps']});"}
        "Export to CSV"]
      [:table#leaderboard
        [:thead
          [:tr [:th ""]
               [:th "Name"]
               [:th "Count"]]]
        [:tbody
          (let [list (cq/integrator-list) count (count list)]
            (for
              [i (range 0 count) :let [record (nth list i)]]
              [:tr.integrator
                [:td.rank.center (inc i)]
                [:td.name {:value (:name record)} (:name record)]
                [:input.email {:type "hidden" :value (:email record)}]
                [:input.id {:type "hidden" :value (:id record)}]
                [:td.center (:count record)]]))]]]))

(defpage "/de-analytics/graph" []
  (render "/de-analytics/graph/day"))

(defpage "/de-analytics/graph/day" []
  (template/graph-page
    [:div.select
      [:input#rb1 {:type "radio" :name "dayGroup" :onClick "setPanSelect()"} "Select&nbsp&nbsp"]
      [:input {:type "radio" :checked "true" :name "dayGroup" :onClick "setPanSelect()"} "Pan"]]
    (template/day-page)))

(defpage "/de-analytics/graph/month" []
  (template/graph-page
    (template/month-page)))

(defpage "/de-analytics/raw" []
  (template/raw-page
    [:h3 "Raw JSON Data:"]
    [:br]
    [:button {:onclick "window.location = '/de-analytics/get-day-data/Completed';"} "Count of Completed apps - By Day"]
    [:button {:onclick "window.location = '/de-analytics/get-day-data/Failed';"} "Count of Failed apps - By Day"]
    [:button {:onclick "window.location = '/de-analytics/get-month-data/Completed';"} "Count of Completed apps - By Month"]
    [:button {:onclick "window.location = '/de-analytics/get-month-data/Failed';"} "Count of Failed apps - By Month"]
    [:button {:onclick "window.location = '/de-analytics/get-historical-app-count';"} "Historical count of apps - By Bucket"]))

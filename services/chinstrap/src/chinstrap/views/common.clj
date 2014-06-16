(ns chinstrap.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page]
        [hiccup.element]))

(defpartial global [title]
  [:head
    [:title (str "DE Analytics - " title)]
    (include-css
      "/de-analytics/css/reset.css"
      "/de-analytics/css/style.css")
    (include-js
      "//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"
      "//ajax.googleapis.com/ajax/libs/jqueryui/1.9.2/jquery-ui.min.js")])

(defpartial graph-nav []
  [:div#graph-nav
    [:span.nav]
    [:a#day.nav {:href "/de-analytics/graph/day"} [:li.nav "Day"]]
    [:span.nav]
    [:a#month.nav {:href "/de-analytics/graph/month"} [:li.nav "Month"]]
    [:span.nav]])

(defpartial navbar []
  [:div#navbar
    [:span.nav]
    [:a#info.nav {:href "/de-analytics/info"} [:li.nav "Info"]]
    [:span.nav]
    [:a#apps.nav {:href "/de-analytics/apps"} [:li.nav "Apps"]]
    [:span.nav]
    [:a#components.nav {:href "/de-analytics/components"} [:li.nav "Components"]]
    [:span.nav]
    [:a#integrators.nav {:href "/de-analytics/integrators"} [:li.nav "Integrators"]]
    [:span.nav]
    [:a#graphs.nav {:href "/de-analytics/graph"} [:li.nav "Graphs"]]
    [:span.nav]])

(defpartial wrapper [& content]
  [:div#wrapper
    (image {:id "logo" :alt "iPlant Logo"} "/de-analytics/img/logo.png")
    [:br]
    content]
  [:br])

(defpartial footer []
  [:div#footer])

(defpartial page [& content]
  (navbar)
  (wrapper content)
  (footer))

(defpartial info-page [& content]
  (html5
    [:head
      (global "Info")
      (include-css "//ajax.googleapis.com/ajax/libs/jqueryui/1.8.2/themes/cupertino/jquery-ui.css")
      (include-js "/de-analytics/js/get-info.js"
                  "/de-analytics/js/info-script.js"
                  "/de-analytics/js/lib/mousetrap.min.js")]
    [:body
      (javascript-tag "$(document).ready(function(){
        $('#info').addClass('active');})")
      (page content)]
    (include-js "/de-analytics/js/lib/csv-parser.js")))

(defpartial apps-page [& content]
  (html5
    [:head
      (global "Apps")
      (include-js "/de-analytics/js/get-apps.js"
                  "/de-analytics/js/collapsible-panel.js")]
    [:body
      (javascript-tag "$(document).ready(function(){
        $('#apps').addClass('active');
        $('.collapsibleContainer').collapsiblePanel();});")
      (page content)]))

(defpartial integrators-page [& content]
  (html5
    [:head
      (global "Integrators")
      (include-css "/de-analytics/css/chosen.css"
                   "/de-analytics/css/pagination.css")
      (include-js "/de-analytics/js/lib/chosen.jquery.min.js"
                  "/de-analytics/js/get-integrators.js"
                  "/de-analytics/js/lib/mousetrap.min.js"
                  "/de-analytics/js/integrators-script.js"
                  "/de-analytics/js/collapsible-panel.js")]
    [:body
      (javascript-tag "$(document).ready(function(){
        $('#integrators').addClass('active');
        $('.chzn-select').chosen({max_selected_options: 1});
        $('.collapsibleContainer').collapsiblePanel();})")
      (page content)]
    (include-js "/de-analytics/js/lib/jquery.dataTables.min.js"
                "/de-analytics/js/lib/csv-parser.js")))

(defpartial components-page [& content]
  (html5
    [:head
      (global "Components")
      (include-js "/de-analytics/js/get-components.js"
                  "/de-analytics/js/collapsible-panel.js")]
    [:body
      (javascript-tag "$(document).ready(function(){
        $('#components').addClass('active');
        $('.collapsibleContainer').collapsiblePanel();});")
      (page content)]
    (include-js "/de-analytics/js/lib/csv-parser.js")))

(defpartial graph-page [& content]
  (html5
    [:head
      (global "Graph - by Day")
      (include-js "/de-analytics/js/lib/spin.min.js"
                  "/de-analytics/js/spinner.js")
      (javascript-tag "$(document).ready(function(){
                       $('#graphs').addClass('active');});")]
    [:body
      {:onload "createChart()"}
      (page
        [:h3
          [:select#type.selector {:onchange "reloadChart()"}
            [:option  {:data ""} "All"]
            [:option {:data "Completed"} "Completed"]
            [:option {:data "Failed"} "Failed"]]
        " DE Apps Over Time"]
        [:br] (graph-nav) [:br]
        [:div#chart]
        [:div#loader]
        content
        [:h5.right "Data Starting from: " [:span#firstDate]])
      (include-js "/de-analytics/js/lib/amcharts.js"
                  "/de-analytics/js/lib/underscore-min.js")]))

(defpartial day-page []
  (include-js "/de-analytics/js/day-graph.js")
  (javascript-tag "$('#day').addClass('active')"))

(defpartial month-page []
  (include-js "/de-analytics/js/month-graph.js")
  (javascript-tag "$('#month').addClass('active')"))

(defpartial raw-page [& content]
  (html5
    [:head
      (global "Raw Data")]
    [:body
      (page content)]))

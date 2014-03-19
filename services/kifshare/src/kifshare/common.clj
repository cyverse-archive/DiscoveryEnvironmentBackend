(ns kifshare.common
  (:require [clojure.string :as string]
            [kifshare.config :as cfg])
  (:use [hiccup.core :only [html]]
        [hiccup.page :only [include-css include-js html5]]))

(defn parse-accept-headers
  "Parses out the accept headers and returns a list
   of the acceptable content types."
  [request]
  (string/split (get-in request [:headers "accept"]) #","))

(defn show-html?
  "Checks to see if 'text/html' is in the list of
   acceptable content-types in the Accept header."
  [request]
  (contains? (set (parse-accept-headers request)) "text/html"))

(defn html-head []
  (html
   [:head
    [:title "iPlant Public Downloads"]
    (map include-css (cfg/css-files))
    (map include-js (cfg/javascript-files))]))

(defn layout [& content]
  (html5
   (html-head)
   [:body
    [:div#wrapper {:id "page-wrapper" :class "container_12"}
     content]]))

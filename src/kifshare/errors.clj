(ns kifshare.errors
  (:use hiccup.core
        [kifshare.common :only [layout]]
        [ring.util.response :only [status]])
  (:require [cheshire.core :as cheshire]))

(def ERR_TICKET_EXPIRED "ERR_TICKET_EXPIRED")
(def ERR_TICKET_USED_UP "ERR_TICKET_USED_UP")
(def ERR_TICKET_NOT_FOUND "ERR_TICKET_NOT_FOUND")
(def ERR_TICKET_NOT_PUBLIC "ERR_TICKET_NOT_PUBLIC")

(defn error-html-page
  [error-msg]
  (layout
   (html
    [:div {:id "err-wrapper"}
     [:div {:id "err-wrapper-inner"}
      error-msg]])))

(defn ticket-expired [] (error-html-page "That ticket has expired."))
(defn ticket-used-up [] (error-html-page "That ticket cannot be used anymore."))
(defn ticket-not-found [] (error-html-page "That ticket does not exist."))

(defn default-error [{:as err-map}]
  (html
   [:div {:id "err-default"}
    [:pre
     [:code (cheshire/encode err-map {:pretty true})]]]))

(defn error-html
  [err-map]
  (let [err-code (:error_code err-map)]
    (cond
     (= err-code ERR_TICKET_NOT_FOUND)
     {:status 404 :body (ticket-not-found)}

     (= err-code ERR_TICKET_EXPIRED)
     {:status 410 :body (ticket-expired)}

     (= err-code ERR_TICKET_USED_UP)
     {:status 410 :body (ticket-used-up)}

     :else
     {:status 500 :body (default-error err-map)})))

(defn error-response
  [err-map]
  (let [err-code (:error_code err-map)]
    (cond
     (= err-code ERR_TICKET_NOT_FOUND)
     {:status 404 :body (ticket-not-found)}

     (= err-code ERR_TICKET_EXPIRED)
     {:status 410 :body (ticket-expired err-map)}

     (= err-code ERR_TICKET_USED_UP)
     {:status 410 :body (ticket-used-up err-map)}

     :else
     {:status 500 :body (default-error err-map)})))

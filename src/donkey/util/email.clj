(ns donkey.util.email
  (:use [donkey.auth.user-attributes :only [current-user]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [donkey.util.config :as config]))

(defn send-email
  "Sends an e-mail message via the iPlant e-mail service."
  [& {:keys [to from-addr from-name subject template values]}]
  (client/post
   (config/iplant-email-base-url)
   {:content-type :json
    :body         (cheshire/encode {:to        to
                                    :from-addr from-addr
                                    :from-name from-name
                                    :subject   subject
                                    :template  template
                                    :values    values})}))

(defn send-tool-request-email
  "Sends the email message informing Core Services of a tool request."
  [tool-req {:keys [firstname lastname email]}]
  (let [template-values {:username           (str firstname " " lastname)
                         :environment        (config/environment-name)
                         :toolrequestid      (:uuid tool-req)
                         :toolrequestdetails (cheshire/encode tool-req {:pretty true})}]
    (send-email
      :to        (config/tool-request-dest-addr)
      :from-addr (config/tool-request-src-addr)
      :subject   "New Tool Request"
      :template  "tool_request"
      :values    template-values)))

(defn- format-question
  "Formats a question and answer for a user feedback submission."
  [[q a]]
  (let [q (string/replace q #"^(?![*])" "* ")
        a (if (string? a) [a] a)]
    (apply str "\n" q "\n" (mapv #(str % "\n") a))))

(defn- feedback-email-text
  "Formats the text for the user feedback email message."
  [feedback]
  (let [user (:username current-user)]
    (apply str user " has provided some DE feedback:\n"
           (mapv format-question feedback))))

(defn send-feedback-email
  "Sends email messages containing user feedback."
  [feedback]
  (let [text (-> (assoc (dissoc feedback "shibbolethEppn")
                   "username" (:shortUsername current-user)
                   "email"    (:email current-user))
                 (feedback-email-text))]
    (send-email
     :to        (config/feedback-dest-addr)
     :from-addr (config/feedback-dest-addr)
     :subject   "DE Feedback"
     :template  "blank"
     :values    {:contents text})))

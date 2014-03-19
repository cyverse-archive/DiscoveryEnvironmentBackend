(ns iplant-email.send-mail
  (:require [clojure.tools.logging :as log])
  (:import [javax.mail Session Message Transport Message$RecipientType]
           [javax.mail.internet InternetAddress MimeMessage]))

(defn set-props
  [host]
  (let [props (System/getProperties)]
    (. props put "mail.smtp.host" host)
    props))

(defn msg->log
  [{:keys [host to-addr from-addr from-name cc-addr subject body]} & 
   {:keys [body?] 
    :or {body? true}}]
  (str "\n**** EMAIL LOG START\n"
       "TO: " to-addr "\n"
       "FROM ADDRESS: " from-addr "\n"
       "FROM NAME: " from-name "\n"
       "CC: " cc-addr "\n"
       "SUBJECT: " subject "\n"
       (when body? 
         (str "BODY: \n" body "\n"))
       "**** EMAIL LOG END\n"))

(defn send-email
  [{:keys [host to-addr from-addr from-name cc-addr subject body] :as req}]
  (let [props   (set-props host)
        session (Session/getDefaultInstance props nil)
        msg     (MimeMessage. session)
        from    (InternetAddress. from-addr)
        to      (InternetAddress. to-addr)
        cc      (when cc-addr (InternetAddress. cc-addr))]
    (when from-name
      (.setPersonal from from-name))
    (doto msg
      (.setFrom from)
      (.addRecipient Message$RecipientType/TO to)
      (.setSubject subject)
      (.setText body))
    (when cc-addr
      (.addRecipient msg Message$RecipientType/CC cc))
    
    (log/warn (msg->log req :body? false))
    
    (try
      (Transport/send msg)
      (catch java.lang.Exception e
        (log/warn "Failed to send message: " e "\n"
                  "******** BEGIN FAILURE\n"
                  (msg->log req)
                  "******** END FAILURE\n")))))

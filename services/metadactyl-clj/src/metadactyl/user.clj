(ns metadactyl.user
  (:use [metadactyl.util.config :only [uid-domain]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as common-errors]
            [metadactyl.util.service :as service]))

(def
  ^{:doc "The authenticated user or nil if the service is unsecured."
    :dynamic true}
   current-user nil)

(defn user-from-attributes
  "Creates an instance of org.iplantc.authn.user.User from the given map."
  [user-attributes]
  (log/debug user-attributes)
  (let [uid (user-attributes :user)]
    (if (empty? uid)
      (throw+ {:error_code common-errors/ERR_NOT_AUTHORIZED,
               :user (dissoc user-attributes :password),
               :message "Invalid user credentials provided."}))
    (-> (select-keys user-attributes [:password :email :first-name :last-name])
        (assoc :username (str uid "@" (uid-domain))
          :shortUsername uid))))

(defmacro with-user
  "Performs a task with the given user information bound to current-user."
  [[user] & body]
  `(binding [current-user (user-from-attributes ~user)]
     (do ~@body)))

(defn store-current-user
  "Creates a function that takes a request, binds current-user to a new instance
   of org.iplantc.authn.user.User that is built from the user attributes found
   in the given params map, then passes request to the given handler."
  [handler & [opts]]
  (fn [{uri :uri :as request}]
    (common-errors/trap uri #(with-user [(:params request)] (handler request)))))

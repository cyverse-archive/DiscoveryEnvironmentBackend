(ns donkey.services.garnish.messages
  (:use [donkey.services.garnish.irods]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.metadata :only [attribute? add-metadata]]
        [donkey.util.config]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.error-codes :as ce]
            [cheshire.core :as json]
            [donkey.services.garnish.irods :as irods]
            [donkey.clients.amqp :as amqp]))

(defn filetype-message-handler
  [payload]
  (try+
    (with-jargon (jargon-cfg) [cm]
      (let [parsed-map (json/parse-string payload true)
            path       (:path parsed-map)]
        (if (nil? path)
          (throw+ {:error_code "ERR_INVALID_JSON"
                   :payload payload}))
        
        (if-not (exists? cm path)
          (log/warn "[filetype-message-handler]" path "does not exist, it probably got moved before the handler fired."))
        
        (if (attribute? cm path (garnish-type-attribute))
          (log/warn "[filetype-message-handler]" path "already has an attribute called" (garnish-type-attribute)))
        
        (when (and (exists? cm path) (not (attribute? cm path (garnish-type-attribute)))) 
          (let [ctype (irods/content-type cm path)]
            (when-not (or (nil? ctype) (string/blank? ctype))
              (log/warn "[filetype-message-handler] adding type" ctype "to" path)
              (add-metadata cm path (garnish-type-attribute) ctype "")
              (log/warn "[filetype-message-handler] done adding type" ctype "to" path))
            (when (or (nil? ctype) (string/blank? ctype))
              (log/warn "[filetype-message-handler] type was not detected for" path))))))
    (catch ce/error? err
      (log/error (ce/format-exception (:throwable &throw-context))))
    (catch Exception e
      (log/error (ce/format-exception e)))))

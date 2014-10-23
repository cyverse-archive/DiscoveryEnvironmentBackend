(ns donkey.routes.notification
  (:use [compojure.core]
        [donkey.services.metadata.metadactyl]
        [donkey.util])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn secured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (GET "/notifications/messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(get-messages req)))

   (GET "/notifications/unseen-messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(get-unseen-messages req)))

   (GET "/notifications/last-ten-messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(last-ten-messages req)))

   (GET "/notifications/count-messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(count-messages req)))

   (POST "/notifications/delete" [:as {:keys [uri] :as req}]
         (ce/trap uri #(delete-notifications req)))

   (DELETE "/notifications/delete-all" [:as {:keys [uri params]}]
           (ce/trap uri #(delete-all-notifications params)))

   (POST "/notifications/seen" [:as {:keys [uri] :as req}]
         (ce/trap uri #(mark-notifications-as-seen req)))

   (POST "/notifications/mark-all-seen" [:as {:keys [uri] :as req}]
         (ce/trap uri #(mark-all-notifications-seen req)))

   (GET "/notifications/system/messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(get-system-messages req)))

   (GET "/notifications/system/new-messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(get-new-system-messages req)))

   (GET "/notifications/system/unseen-messages" [:as {:keys [uri] :as req}]
        (ce/trap uri #(get-unseen-system-messages req)))

   (POST "/notifications/system/received" [:as {:keys [uri] :as req}]
         (ce/trap uri #(mark-system-messages-received req)))

   (POST "/notifications/system/mark-all-received" [:as {:keys [uri] :as req}]
         (ce/trap uri #(mark-all-system-messages-received req)))

   (POST "/notifications/system/seen" [:as {:keys [uri] :as req}]
         (ce/trap uri #(mark-system-messages-seen req)))

   (POST "/notifications/system/mark-all-seen" [:as {:keys [uri] :as req}]
         (ce/trap uri #(mark-all-system-messages-seen req)))

   (POST "/notifications/system/delete" [:as {:keys [uri] :as req}]
         (ce/trap uri #(delete-system-messages req)))

   (DELETE "/notifications/system/delete-all" [:as {:keys [uri] :as req}]
           (ce/trap uri #(delete-all-system-messages req)))

   (PUT "/notifications/admin/system" [:as {:keys [uri] :as req}]
        (ce/trap uri #(admin-add-system-message req)))

   (GET "/notifications/admin/system" [:as {:keys [uri] :as req}]
       (ce/trap uri #(admin-list-system-messages req)))

   (GET "/notifications/admin/system/:uuid" [uuid :as {:keys [uri] :as req}]
        (ce/trap uri #(admin-get-system-message req uuid)))

   (POST "/notifications/admin/system/:uuid" [uuid :as {:keys [uri] :as req}]
         (ce/trap uri #(admin-update-system-message req uuid)))

   (DELETE "/notifications/admin/system/:uuid" [uuid :as {:keys [uri] :as req}]
           (ce/trap uri #(admin-delete-system-message req uuid)))

   (GET "/notifications/admin/system-types" [:as {:keys [uri] :as req}]
        (ce/trap uri #(admin-list-system-types req)))))

(defn unsecured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (POST "/send-notification" [:as {:keys [uri] :as req}]
         (ce/trap uri #(send-notification req)))))

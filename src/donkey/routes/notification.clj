(ns donkey.routes.notification
  (:use [compojure.core]
        [donkey.services.metadata.metadactyl]
        [donkey.util])
  (:require [clojure.tools.logging :as log]
            [donkey.util.config :as config]))

(defn secured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (GET "/notifications/messages" [:as req]
        (trap #(get-messages req)))

   (GET "/notifications/unseen-messages" [:as req]
        (trap #(get-unseen-messages req)))

   (GET "/notifications/last-ten-messages" [:as req]
        (trap #(last-ten-messages req)))

   (GET "/notifications/count-messages" [:as req]
        (trap #(count-messages req)))

   (POST "/notifications/delete" [:as req]
         (trap #(delete-notifications req)))

   (DELETE "/notifications/delete-all" [:as {params :params}]
           (trap #(delete-all-notifications params)))

   (POST "/notifications/seen" [:as req]
         (trap #(mark-notifications-as-seen req)))

   (POST "/notifications/mark-all-seen" [:as req]
         (trap #(mark-all-notifications-seen req)))

   (GET "/notifications/system/messages" [:as req]
        (trap #(get-system-messages req)))

   (GET "/notifications/system/new-messages" [:as req]
        (trap #(get-new-system-messages req)))

   (GET "/notifications/system/unseen-messages" [:as req]
        (trap #(get-unseen-system-messages req)))

   (POST "/notifications/system/received" [:as req]
         (trap #(mark-system-messages-received req)))

   (POST "/notifications/system/mark-all-received" [:as req]
         (trap #(mark-all-system-messages-received req)))

   (POST "/notifications/system/seen" [:as req]
         (trap #(mark-system-messages-seen req)))

   (POST "/notifications/system/mark-all-seen" [:as req]
         (trap #(mark-all-system-messages-seen req)))

   (POST "/notifications/system/delete" [:as req]
         (trap #(delete-system-messages req)))

   (DELETE "/notifications/system/delete-all" [:as req]
           (trap #(delete-all-system-messages req)))

   (PUT "/notifications/admin/system" [:as req]
        (trap #(admin-add-system-message req)))

   (GET "/notifications/admin/system" [:as req]
       (trap #(admin-list-system-messages req)))

   (GET "/notifications/admin/system/:uuid" [uuid :as req]
        (trap #(admin-get-system-message req uuid)))

   (POST "/notifications/admin/system/:uuid" [uuid :as req]
         (trap #(admin-update-system-message req uuid)))

   (DELETE "/notifications/admin/system/:uuid" [uuid :as req]
           (trap #(admin-delete-system-message req uuid)))

   (GET "/notifications/admin/system-types" [:as req]
        (trap #(admin-list-system-types req)))))

(defn unsecured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (POST "/send-notification" [:as req]
         (trap #(send-notification req)))))

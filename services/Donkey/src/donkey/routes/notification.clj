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
        (get-messages req))

   (GET "/notifications/unseen-messages" [:as req]
        (get-unseen-messages req))

   (GET "/notifications/last-ten-messages" [:as req]
        (last-ten-messages req))

   (GET "/notifications/count-messages" [:as req]
        (count-messages req))

   (POST "/notifications/delete" [:as req]
         (delete-notifications req))

   (DELETE "/notifications/delete-all" [:as {params :params}]
           (delete-all-notifications params))

   (POST "/notifications/seen" [:as req]
         (mark-notifications-as-seen req))

   (POST "/notifications/mark-all-seen" [:as req]
         (mark-all-notifications-seen req))

   (GET "/notifications/system/messages" [:as req]
        (get-system-messages req))

   (GET "/notifications/system/new-messages" [:as req]
        (get-new-system-messages req))

   (GET "/notifications/system/unseen-messages" [:as req]
        (get-unseen-system-messages req))

   (POST "/notifications/system/received" [:as req]
         (mark-system-messages-received req))

   (POST "/notifications/system/mark-all-received" [:as req]
         (mark-all-system-messages-received req))

   (POST "/notifications/system/seen" [:as req]
         (mark-system-messages-seen req))

   (POST "/notifications/system/mark-all-seen" [:as req]
         (mark-all-system-messages-seen req))

   (POST "/notifications/system/delete" [:as req]
         (delete-system-messages req))

   (DELETE "/notifications/system/delete-all" [:as req]
           (delete-all-system-messages req))))

(defn admin-notification-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/notification-routes-enabled))]

    (PUT "/notifications/system" [:as req]
         (admin-add-system-message req))

    (GET "/notifications/system" [:as req]
         (admin-list-system-messages req))

    (GET "/notifications/system/:uuid" [uuid :as req]
         (admin-get-system-message req uuid))

    (POST "/notifications/system/:uuid" [uuid :as req]
          (admin-update-system-message req uuid))

    (DELETE "/notifications/system/:uuid" [uuid :as req]
            (admin-delete-system-message req uuid))

    (GET "/notifications/system-types" [:as req]
         (admin-list-system-types req))))

(defn unsecured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (POST "/send-notification" [:as req]
         (send-notification req))))

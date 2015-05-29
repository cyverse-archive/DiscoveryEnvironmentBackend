(ns donkey.routes.notification
  (:use [compojure.core]
        [donkey.util])
  (:require [clojure.tools.logging :as log]
            [donkey.clients.notifications.raw :as rn]
            [donkey.clients.notifications :as cn]
            [donkey.util.config :as config]
            [donkey.util.service :as service]))

(defn secured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (GET "/notifications/messages" [:as {:keys [params]}]
        (service/success-response (rn/get-messages params)))

   (GET "/notifications/unseen-messages" [:as {:keys [params]}]
        (service/success-response (rn/get-unseen-messages params)))

   (GET "/notifications/last-ten-messages" []
        (service/success-response (rn/last-ten-messages)))

   (GET "/notifications/count-messages" [:as {:keys [params]}]
        (service/success-response (rn/count-messages params)))

   (POST "/notifications/delete" [:as {:keys [body]}]
         (service/success-response (rn/delete-notifications body)))

   (DELETE "/notifications/delete-all" [:as {:keys [params]}]
           (service/success-response (rn/delete-all-notifications params)))

   (POST "/notifications/seen" [:as {:keys [body]}]
         (service/success-response (rn/mark-notifications-seen body)))

   (POST "/notifications/mark-all-seen" []
         (service/success-response (cn/mark-all-notifications-seen)))

   (GET "/notifications/system/messages" []
        (service/success-response (rn/get-system-messages)))

   (GET "/notifications/system/new-messages" []
        (service/success-response (rn/get-new-system-messages)))

   (GET "/notifications/system/unseen-messages" []
        (service/success-response (rn/get-unseen-system-messages)))

   (POST "/notifications/system/received" [:as {:keys [body]}]
         (service/success-response (rn/mark-system-messages-received body)))

   (POST "/notifications/system/mark-all-received" [:as {:keys [body]}]
         (service/success-response (rn/mark-all-system-messages-received body)))

   (POST "/notifications/system/seen" [:as {:keys [body]}]
         (service/success-response (rn/mark-system-messages-seen body)))

   (POST "/notifications/system/mark-all-seen" [:as {:keys [body]}]
         (service/success-response (rn/mark-all-system-messages-seen body)))

   (POST "/notifications/system/delete" [:as {:keys [body]}]
         (service/success-response (rn/delete-system-messages body)))

   (DELETE "/notifications/system/delete-all" [:as {:keys [params]}]
           (service/success-response (rn/delete-all-system-messages params)))))

(defn admin-notification-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/notification-routes-enabled))]

    (PUT "/notifications/system" [:as {:keys [body]}]
         (service/success-response (rn/admin-add-system-message body)))

    (GET "/notifications/system" [:as {:keys [params]}]
         (service/success-response (rn/admin-list-system-messages params)))

    (GET "/notifications/system/:uuid" [uuid]
         (service/success-response (rn/admin-get-system-message uuid)))

    (POST "/notifications/system/:uuid" [uuid :as {:keys [body]}]
          (service/success-response (rn/admin-update-system-message uuid body)))

    (DELETE "/notifications/system/:uuid" [uuid]
            (service/success-response (rn/admin-delete-system-message uuid)))

    (GET "/notifications/system-types" []
         (service/success-response (rn/admin-list-system-types)))))

(defn unsecured-notification-routes
  []
  (optional-routes
   [config/notification-routes-enabled]

   (POST "/send-notification" [:as {:keys [body]}]
         (service/success-response (rn/send-notification body)))))

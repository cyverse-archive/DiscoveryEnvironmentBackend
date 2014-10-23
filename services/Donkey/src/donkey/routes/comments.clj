(ns donkey.routes.comments
  (:use [compojure.core :only [GET PATCH POST]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.services.metadata.comments :as comments]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-comment-routes
  []
  (util/optional-routes
    [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

    (GET "/filesystem/entry/:entry-id/comments" [entry-id :as {:keys [uri]}]
         (ce/trap uri #(comments/list-comments entry-id)))

    (POST "/filesystem/entry/:entry-id/comments" [entry-id :as {:keys [uri body]}]
          (ce/trap uri #(comments/add-comment entry-id body)))

    ;; TODO: determine where the value of "retracted" comes from.
    (PATCH "/filesystem/entry/:entry-id/comments/:comment-id"
           [entry-id comment-id retracted :as {:keys [uri]}]
           (ce/trap uri #(comments/update-retract-status entry-id comment-id retracted)))))

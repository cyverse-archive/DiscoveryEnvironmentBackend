(ns donkey.routes.tags
  (:use [compojure.core :only [DELETE GET PATCH POST]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.services.metadata.tags :as tags]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-tag-routes
  []
  (util/optional-routes
    [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

    (GET "/filesystem/entry/:entry-id/tags" [entry-id :as {:keys [uri]}]
         (ce/trap uri #(tags/list-attached-tags entry-id)))

    (PATCH "/filesystem/entry/:entry-id/tags" [entry-id type :as {:keys [uri body]}]
           (ce/trap uri #(tags/handle-patch-file-tags entry-id type body)))

    (GET "/tags/suggestions" [contains limit :as {:keys [uri]}]
         (ce/trap uri #(tags/suggest-tags contains limit)))

    (POST "/tags/user" [:as {:keys [uri body]}]
          (ce/trap uri #(tags/create-user-tag body)))

    (PATCH "/tags/user/:tag-id" [tag-id :as {:keys [uri body]}]
           (ce/trap uri #(tags/update-user-tag tag-id body)))

    (DELETE "/tags/user/:tag-id" [tag-id :as {:keys [uri]}]
            (ce/trap uri #(tags/delete-user-tag tag-id)))))

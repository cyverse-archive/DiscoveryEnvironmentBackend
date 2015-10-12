(ns donkey.routes.favorites
  (:use [compojure.core :only [DELETE GET POST PUT]])
  (:require [donkey.services.metadata.favorites :as fave]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-favorites-routes
  []
  (util/optional-routes
   [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

   (PUT "/favorites/filesystem/:entry-id" [entry-id]
        (fave/add-favorite entry-id))

   (DELETE "/favorites/filesystem/:entry-id" [entry-id]
           (fave/remove-favorite entry-id))

   (GET "/favorites/filesystem" [sort-col sort-dir limit offset entity-type info-type]
        (fave/list-favorite-data-with-stat sort-col sort-dir limit offset entity-type info-type))

   (POST "/favorites/filter" [:as {body :body}]
         (fave/filter-accessible-favorites body))))

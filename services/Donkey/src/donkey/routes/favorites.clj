(ns donkey.routes.favorites
  (:use [compojure.core :only [DELETE GET POST PUT]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.services.metadata.favorites :as fave]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-favorites-routes
  []
  (util/optional-routes
   [#(and (config/filesystem-routes-enabled) (config/metadata-routes-enabled))]

   (PUT "/favorites/filesystem/:entry-id" [entry-id :as {:keys [uri]}]
        (ce/trap uri #(fave/add-favorite entry-id)))

   (DELETE "/favorites/filesystem/:entry-id" [entry-id :as {:keys [uri]}]
           (ce/trap uri #(fave/remove-favorite entry-id)))

   (GET "/favorites/filesystem" [sort-col sort-order limit offset :as {:keys [uri]}]
        (ce/trap uri #(fave/list-favorite-data-with-stat sort-col sort-order limit offset)))

   (POST "/favorites/filter" [:as {:keys [uri body]}]
         (ce/trap uri #(fave/filter-favorites body)))))

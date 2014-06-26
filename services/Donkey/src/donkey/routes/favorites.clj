(ns donkey.routes.favorites
  (:use [compojure.core :only [DELETE GET POST PUT]])
  (:require [donkey.services.metadata.favorites :as fave]
            [donkey.util :as util]
            [donkey.util.config :as config]))


(defn secured-favorites-routes
  []
  (util/optional-routes
   [config/metadata-routes-enabled]

   (PUT "/favorites/filesystem/:entry-id" [entry-id]
        (util/trap #(fave/add-favorite entry-id)))
   
   (DELETE "/favorites/filesystem/:entry-id" [entry-id]
           (util/trap #(fave/remove-favorite entry-id)))
   
   (GET "/favorites/filesystem" [sort-col sort-order limit offset]
        (util/trap #(fave/list-favorite-data-with-stat sort-col sort-order limit offset)))
   
   (POST "/favorites/filter" [:as {body :body}]
         (util/trap #(fave/filter-favorites body)))))

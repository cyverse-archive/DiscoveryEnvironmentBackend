(ns donkey.routes.pref
  (:use [compojure.core]
        [donkey.services.user-prefs]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn secured-pref-routes
  []
  (optional-routes
   [config/pref-routes-enabled]

   (GET "/preferences" []
        (do-get-prefs))

   (POST "/preferences" [:as {body :body}]
         (do-post-prefs (slurp body)))

   (DELETE "/preferences" []
           (remove-prefs))

   #_(GET "/search-history" []
        (search-history))

   #_(POST "/search-history" [:as {body :body}]
         (search-history (slurp body)))

   #_(DELETE "/search-history" []
           (clear-search-history))))

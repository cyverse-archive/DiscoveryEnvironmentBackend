(ns donkey.routes.pref
  (:use [compojure.core]
        [donkey.services.user-prefs]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn secured-pref-routes
  []
  (optional-routes
   [config/pref-routes-enabled]

   (GET "/preferences" []
        (trap user-prefs))

   (POST "/preferences" [:as {body :body}]
         (trap #(user-prefs (slurp body))))

   (DELETE "/preferences" []
           (trap remove-prefs))

   (GET "/search-history" []
        (trap search-history))

   (POST "/search-history" [:as {body :body}]
         (trap #(search-history (slurp body))))

   (DELETE "/search-history" []
           (trap clear-search-history))))

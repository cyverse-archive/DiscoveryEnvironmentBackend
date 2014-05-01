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
        (trap do-get-prefs))

   (POST "/preferences" [:as {body :body}]
         (trap #(do-post-prefs (slurp body))))

   (DELETE "/preferences" []
           (trap remove-prefs))

   #_(GET "/search-history" []
        (trap search-history))

   #_(POST "/search-history" [:as {body :body}]
         (trap #(search-history (slurp body))))

   #_(DELETE "/search-history" []
           (trap clear-search-history))))

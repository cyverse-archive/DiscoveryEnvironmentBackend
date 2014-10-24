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

   (GET "/preferences" [:as {:keys [uri]}]
        (ce/trap uri do-get-prefs))

   (POST "/preferences" [:as {:keys [uri body]}]
         (ce/trap uri #(do-post-prefs (slurp body))))

   (DELETE "/preferences" [:as {:keys [uri]}]
           (ce/trap uri remove-prefs))

   #_(GET "/search-history" [:as {:keys [uri]}]
        (ce/trap uri search-history))

   #_(POST "/search-history" [:as {:keys [uri body]}]
         (ce/trap uri #(search-history (slurp body))))

   #_(DELETE "/search-history" [:as {:keys [uri]}]
           (ce/trap uri clear-search-history))))

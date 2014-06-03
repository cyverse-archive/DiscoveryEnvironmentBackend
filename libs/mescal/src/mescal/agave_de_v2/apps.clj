(ns mescal.agave-de-v2.apps
  (:use [mescal.agave-de-v2.app-listings :only [get-app-name]])
  (:require [mescal.agave-de-v2.constants :as c]
            [mescal.util :as util]))

(defn format-groups
  [app irods-home]
  {})

(defn format-app
  [app irods-home]
  (let [app-label (get-app-name app)]
    {:id           (:id app)
     :name         app-label
     :label        app-label
     :component_id c/hpc-group-id
     :groups       (format-groups irods-home app)}))

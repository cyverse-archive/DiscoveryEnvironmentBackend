(ns clavin.loader
  (:require [clavin.generator :as gen]
            [clavin.properties :as props]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.props :as ccprops]
            [clojure.string :as string]))

(def prop-hosts
  #(vec (map string/trim (string/split (get %1 %2) #","))))

(defn- set-of-hosts
  [acl-props]
  (set
   (flatten
    (vec (for [ak (keys acl-props)]
           (prop-hosts acl-props ak))))))

(defn- list-of-host-deployments
  [host acl-props]
  (vec (filter
        #(contains? (set (prop-hosts acl-props %1)) host)
        (keys acl-props))))

(defn- host-map
  [host acl-props]
  {host (list-of-host-deployments host acl-props)})

(defn all-host-maps
  [acl-props]
  (let [hosts      (set-of-hosts acl-props)]
    (apply merge (for [h hosts] (host-map h acl-props)))))

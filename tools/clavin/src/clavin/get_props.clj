(ns clavin.get-props
  (:require [clavin.zk :as zk]
            [clojure.string :as string]))

(defn- zk-path
  "Builds a Zookeeper path from one or more path components."
  [& components]
  (->> components
       (map #(string/replace % #"\A/|/\z" ""))
       (cons "")
       (string/join "/")))

(defn- dep-path
  "Obtains a path to use to get a configuration setting for a deployment."
  [deployment service & more]
  (apply zk-path (string/replace deployment #"[.]" "/") service more))

(defn get-prop
  "Gets a single property value from Zookeeper.  This function should be called
   from within a with-zk macro.

   Parameters:
     deployment - the deployment (for example, de.dev.de-2)
     service    - the service name (for example, donkey)
     prop-name  - the property name (for example, donkey.cas.server-name)

   Returns the property value or nil if the property isn't defined."
  [deployment service prop-name]
  (or (zk/get-value (dep-path deployment service prop-name)) ""))

(defn- get-prop-names
  "Gets the list of property names to display for get-props.  If property
   names are explicitly specified then those property names will be used.
   Otherwise, all property names for the service will be used."
  [base-path prop-names]
  (if (empty? prop-names)
    (zk/list-children base-path)
    prop-names))

(defn get-props
  "Gets one or more property values from Zookeeper.  This function should be
   called from within a with-zk macro.

   Parameters:
     deployment - the deployment (for example, de.dev.de-2)
     service    - the service name (for example, donkey)
     prop-names - the list of property names (all props if empty)

   Returns a map of property names to property values."
  [deployment service prop-names]
  (let [base-path  (dep-path deployment service)]
    (->> (get-prop-names base-path prop-names)
         (map #(vector % (or (zk/get-value (zk-path base-path %)) "")))
         (into (sorted-map)))))

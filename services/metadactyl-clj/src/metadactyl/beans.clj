(ns metadactyl.beans
  (:require [clojure.tools.logging :as log])
  (:import [java.util Properties]))

(def
  ^{:private true
    :doc "The list of initialization functions to call."}
   beans (ref []))

(defmacro defbean
  "Defines a bean that will be available for use in services."
  [sym docstr & init-forms]
  `(def ~(with-meta sym {:doc docstr}) (memoize (fn [] ~@init-forms))))

(defn register-bean
  "Registers a bean for initialization."
  [new-bean]
  (dosync (alter beans conj new-bean)))

(defn init-bean
  "Initializes a single bean and logs the bean value if debugging is enabled."
  [f]
  (let [java-bean (f)]
    (log/warn "BEAN -" (str (:name (meta f)) ":") java-bean)))

(defn init-registered-beans
  "Initializes all registered-beans."
  []
  (dorun (map #(init-bean %) @beans)))

(defn as-properties
  "Converts a map to an instance of Java.util.Properties."
  [m]
  (let [props (Properties.)]
    (dorun (map #(.setProperty props (key %) (val %)) m))
    props))

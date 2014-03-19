(ns metadactyl.translations.app-metadata.external-to-preview
  (:use [metadactyl.translations.app-metadata.util])
  (:require [clojure.string :as string]))

(defn- default-prop-translation
  ([prop]
     (default-prop-translation prop #(:value % "")))
  ([prop f]
     [{:name  (:name prop "--unnamed-parameter")
       :value (f prop)
       :order (:order prop 0)}]))

(defn- flag-prop-translation
  [prop]
  (when (Boolean/parseBoolean (str (:value prop "false")))
    (default-prop-translation prop (constantly ""))))

(defn- prop-from-selection
  [{:keys [name value]
    :or {name  ""
         value ""}}
   order]
  (when (or (seq name) (seq value))
    {:name  name
     :value value
     :order order}))

(defn- tree-selection-prop-translation
  [prop]
  (let [values (:defaultValue prop)]
    (when (sequential? values)
      (map #(prop-from-selection % (:order prop 0)) values))))

(defn- selection-prop-translation
  [prop]
  [(prop-from-selection
    (:defaultValue prop "")
    (:order prop 0))])

(defn- multi-file-input-prop-translation
  [prop]
  (mapcat #(default-prop-translation prop (constantly %))
          (:value prop)))

(defn- output-prop-translation
  [prop]
  (let [data-obj (:data_object prop)]
    (when (and (not (:is_implicit data-obj)) (= (:data_source data-obj) "file"))
      (default-prop-translation prop))))

(def ^:private prop-translation-fns
  [[#(= "Flag" %)                flag-prop-translation]
   [#(= "TreeSelection" %)       tree-selection-prop-translation]
   [#(re-find #"Selection$" %)   selection-prop-translation]
   [#(= "MultiFileSelector" %)   multi-file-input-prop-translation]
   [#(re-find #"Output$" %)      output-prop-translation]
   [#(= "EnvironmentVariable" %) (constantly nil)]
   [(constantly true)            default-prop-translation]])

(defn- get-prop-translation-fn
  [{:keys [type] :or {type ""}}]
  (->> (map (fn [[p f]] (when (p type) f)) prop-translation-fns)
       (remove nil?)
       (first)))

(defn- translate-prop
  [prop]
  ((get-prop-translation-fn prop) prop))

(defn- translate-prop-group
  [group]
  (mapcat translate-prop (:properties group)))

(defn translate-template
  [template]
  {:params
   (->> (get-property-groups template)
        (mapcat translate-prop-group)
        (remove nil?))})

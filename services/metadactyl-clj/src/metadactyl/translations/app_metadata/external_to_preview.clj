(ns metadactyl.translations.app-metadata.external-to-preview
  (:use [metadactyl.translations.app-metadata.util])
  (:require [clojure.string :as string]
            [me.raynes.fs :as fs]))

(defn- base-name
  [path]
  (when-not (string/blank? path)
    (fs/base-name path)))

(defn- default-prop-value-fn
  [prop]
  (:value prop ""))

(defn- implicit?
  [prop]
  (get-in prop [:file_parameters :is_implicit] false))

(defn- data-source
  [prop]
  (get-in prop [:file_parameters :data_source]))

(defn- default-prop-translation
  ([prop]
     (default-prop-translation prop default-prop-value-fn))
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

(defn- input-prop-translation
  ([prop]
     (input-prop-translation prop default-prop-value-fn))
  ([prop f]
     (let [path (base-name (:path (f prop)))]
       (when-not (or (implicit? prop) (string/blank? path))
         (default-prop-translation prop (constantly path))))))

(defn- multi-file-input-prop-translation
  [prop]
  (mapcat #(input-prop-translation prop (constantly %))
          (:value prop)))

(defn- output-prop-default-value
  [prop]
  (let [default-value (:value prop)]
    (if (string/blank? default-value) "file" default-value)))

(defn- output-prop-translation
  [prop]
  (when (and (not (implicit? prop)) (= (data-source prop) "file"))
    (default-prop-translation prop output-prop-default-value)))

(def ^:private prop-translation-fns
  [[#(= "Flag" %)                flag-prop-translation]
   [#(= "TreeSelection" %)       tree-selection-prop-translation]
   [#(re-find #"Selection$" %)   selection-prop-translation]
   [#(re-find #"Input$" %)       input-prop-translation]
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
  (mapcat translate-prop (:parameters group)))

(defn translate-template
  [template]
  {:params
   (->> (:groups template)
        (mapcat translate-prop-group)
        (remove nil?))})

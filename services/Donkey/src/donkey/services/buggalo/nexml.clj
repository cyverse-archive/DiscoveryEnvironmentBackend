(ns donkey.services.buggalo.nexml
  (:use [clojure.java.io :only [file reader writer]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [javax.xml XMLConstants]
           [javax.xml.transform.stream StreamSource]
           [javax.xml.validation SchemaFactory]
           [org.nexml.model DocumentFactory Tree]
           [org.xml.sax ErrorHandler]))

(defn- get-resource
  "Loads a resource from the classpath."
  [resource-name]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResource resource-name)))

(defn- xsd-error-handler
  "Creates an error handler that can be used during the validation of an XML document."
  [is-valid?]
  (reify ErrorHandler
    (error [this e]
      (log/debug "XML schema validation error:" e)
      (reset! is-valid? false))
    (fatalError [this e]
      (log/debug "fatal XML schema validation error:" e)
      (reset! is-valid? false))
    (warning [this e]
      (log/debug "XML schema validation warning:" e))))

(defn- load-schema
  "Loads an XML schema from a file somewhere on the classpath."
  [path]
  (.newSchema (SchemaFactory/newInstance XMLConstants/W3C_XML_SCHEMA_NS_URI)
              (get-resource path)))

(defn- first-line
  "Loads the firt non-blank line from a file."
  [f]
  (with-open [rdr (reader f)]
    (first (remove string/blank? (line-seq rdr)))))

(defn is-nexml?
  "Determines if a file is a NeXML file."
  [infile]
  (when (re-find #"^<" (first-line infile))
    (let [schema    (load-schema "nexml/xsd/nexml.xsd")
          is-valid? (atom true)
          validator (.newValidator schema)]
      (.setErrorHandler validator (xsd-error-handler is-valid?))
      (.validate validator (StreamSource. infile))
      @is-valid?)))

(defn format-node
  "Serializes a NeXML tree node as a Newick string and writes the resulting
   string to a writer."
  [out tree parent node]
  (let [children   (sort-by #(.getId %) (.getOutNodes tree node))
        label      (.getLabel node)
        branch-len (when-not (nil? parent) (.getLength (.getEdge tree parent node)))]
    (when-not (empty? children)
      (.write out "(")
      (loop [[child & more] children]
        (format-node out tree parent child)
        (when more
          (.write out ",")
          (recur more)))
      (.write out ")"))
    (.write out label)
    (when-not (nil? branch-len)
      (.write out ":")
      (.write out (str branch-len)))))

(defn- to-newick
  "Writes a newick string for a NeXML tree to a writer."
  [out tree]
  (let [count-parents #(count (seq (.getInNodes tree %)))
        root (or (.getRoot tree)
                 (first (filter #(zero? (count-parents %)) (.getNodes tree))))]
    (format-node out tree nil root)))

(defn- save-tree-file
  "Saves a NeXML tree to a Newick file."
  [dir index tree]
  (let [label    (.getLabel tree)
        filename (if (string/blank? label)
                   (str "tree_" index ".tre")
                   (str label ".tre"))
        out-file (file dir filename)]
    (with-open [out (writer out-file)]
      (to-newick out tree)
      (.write out ";")
      (.write out (System/getProperty "line.separator")))
    out-file))

(defn extract-trees-from-nexml
  "Extracts all trees from a NeXML file."
  [dir infile]
  (let [networks (mapcat seq (.getTreeBlockList (DocumentFactory/parse infile)))
        trees    (filter (partial instance? Tree) networks)]
    (when (empty? trees)
      (throw (IllegalArgumentException. "no trees found in NeXML file")))
    (mapv (partial save-tree-file dir) (range) trees)))

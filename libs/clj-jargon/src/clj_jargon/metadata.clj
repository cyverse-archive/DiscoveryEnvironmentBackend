(ns clj-jargon.metadata
  (:use [clj-jargon.validations]
        [clj-jargon.item-info :only [is-dir?]])
  (:require [clojure.string :as string])
  (:import [org.irods.jargon.core.pub.domain AvuData]
           [org.irods.jargon.core.query IRODSGenQueryBuilder]
           [org.irods.jargon.core.query QueryConditionOperators]
           [org.irods.jargon.core.query RodsGenQueryEnum]
           [org.irods.jargon.core.query AVUQueryElement]
           [org.irods.jargon.core.query AVUQueryElement$AVUQueryPart]
           [org.irods.jargon.core.query AVUQueryOperatorEnum]))

(defn map2avu
  [avu-map]
  "Converts an avu map into an AvuData instance."
  (AvuData/instance (:attr avu-map) (:value avu-map) (:unit avu-map)))

(defn get-metadata
  [cm dir-path]
  "Returns all of the metadata associated with a path."
  (validate-path-lengths dir-path)
  (mapv
    #(hash-map :attr  (.getAvuAttribute %1)
               :value (.getAvuValue %1)
               :unit  (.getAvuUnit %1))
    (if (is-dir? cm dir-path)
      (.findMetadataValuesForCollection (:collectionAO cm) dir-path)
      (.findMetadataValuesForDataObject (:dataObjectAO cm) dir-path))))

(defn get-attribute
  [cm dir-path attr]
  "Returns a list of avu maps for set of attributes associated with dir-path"
  (validate-path-lengths dir-path)
  (filter
    #(= (:attr %1) attr)
    (get-metadata cm dir-path)))

(defn get-attribute-value
  [cm apath attr val]
  (validate-path-lengths apath)
  (filter
    #(and (= (:attr %1) attr)
          (= (:value %1) val))
    (get-metadata cm apath)))

(defn attribute?
  [cm dir-path attr]
  "Returns true if the path has the associated attribute."
  (validate-path-lengths dir-path)
  (pos? (count (get-attribute cm dir-path attr))))

(defn attr-value?
  "Returns a truthy value if path has metadata that has an attribute of attr and
   a value of val."
  [cm path attr val]
  (-> (filter
        #(and (= (:attr %1) attr)
              (= (:value %1) val))
        (get-metadata cm path))
    count
    pos?))

(defn add-metadata
  [cm dir-path attr value unit]
  (validate-path-lengths dir-path)
  (let [ao-obj (if (is-dir? cm dir-path)
                 (:collectionAO cm)
                 (:dataObjectAO cm))]
    (.addAVUMetadata ao-obj dir-path (AvuData/instance attr value unit))))

(defn set-metadata
  [cm dir-path attr value unit]
  "Sets an avu for dir-path."
  (validate-path-lengths dir-path)
  (let [avu    (AvuData/instance attr value unit)
        ao-obj (if (is-dir? cm dir-path)
                 (:collectionAO cm)
                 (:dataObjectAO cm))]
    (if (zero? (count (get-attribute cm dir-path attr)))
      (.addAVUMetadata ao-obj dir-path avu)
      (let [old-avu (map2avu (first (get-attribute cm dir-path attr)))]
        (.modifyAVUMetadata ao-obj dir-path old-avu avu)))))

(defn- delete-meta
  [cm dir-path attr-func]
  (validate-path-lengths dir-path)
  (let [fattr  (first (attr-func))
        avu    (map2avu fattr)
        ao-obj (if (is-dir? cm dir-path)
                 (:collectionAO cm)
                   (:dataObjectAO cm))]
    (.deleteAVUMetadata ao-obj dir-path avu)))

(defn delete-metadata
  ([cm dir-path attr]
    (delete-meta cm dir-path #(get-attribute cm dir-path attr)))
  ([cm dir-path attr val]
    (delete-meta cm dir-path #(get-attribute-value cm dir-path attr val))))

(defn delete-avus
  [cm dir-path avu-maps]
  (validate-path-lengths dir-path)
  (let [ao (if (is-dir? cm dir-path) (:collectionAO cm) (:dataObjectAO cm))]
    (doseq [avu-map avu-maps]
      (when (attr-value? cm dir-path (:attr avu-map) (:value avu-map))
        (.deleteAVUMetadata ao dir-path (map2avu avu-map))))))

(defn- op->constant
  [op]
  (or ({:between         QueryConditionOperators/BETWEEN
        :=               QueryConditionOperators/EQUAL
        :>               QueryConditionOperators/GREATER_THAN
        :>=              QueryConditionOperators/GREATER_THAN_OR_EQUAL_TO
        :in              QueryConditionOperators/IN
        :<               QueryConditionOperators/LESS_THAN
        :<=              QueryConditionOperators/LESS_THAN_OR_EQUAL_TO
        :like            QueryConditionOperators/LIKE
        :not-between     QueryConditionOperators/NOT_BETWEEN
        :not=            QueryConditionOperators/NOT_EQUAL
        :not-in          QueryConditionOperators/NOT_IN
        :not-like        QueryConditionOperators/NOT_LIKE
        :num=            QueryConditionOperators/NUMERIC_EQUAL
        :num>            QueryConditionOperators/NUMERIC_GREATER_THAN
        :num>=           QueryConditionOperators/NUMERIC_GREATER_THAN_OR_EQUAL_TO
        :num<            QueryConditionOperators/NUMERIC_LESS_THAN
        :num<=           QueryConditionOperators/NUMERIC_LESS_THAN_OR_EQUAL_TO
        :sounds-like     QueryConditionOperators/SOUNDS_LIKE
        :sounds-not-like QueryConditionOperators/SOUNDS_NOT_LIKE
        :table           QueryConditionOperators/TABLE} op)
      (throw (Exception. (str "unknown operator: " op)))))

(defn- build-file-avu-query
  [name op value]
  (-> (IRODSGenQueryBuilder. true nil)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_COLL_NAME)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_DATA_NAME)
      (.addConditionAsGenQueryField RodsGenQueryEnum/COL_META_DATA_ATTR_NAME
                                    QueryConditionOperators/EQUAL name)
      (.addConditionAsGenQueryField RodsGenQueryEnum/COL_META_DATA_ATTR_VALUE
                                    (op->constant op)
                                    (str value))
      (.exportIRODSQueryFromBuilder 50000)))

(defn- build-file-attr-query
  [name]
  (-> (IRODSGenQueryBuilder. true nil)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_COLL_NAME)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_DATA_NAME)
      (.addConditionAsGenQueryField RodsGenQueryEnum/COL_META_DATA_ATTR_NAME
                                    QueryConditionOperators/EQUAL name)
      (.exportIRODSQueryFromBuilder 50000)))

(defn build-query-for-avu-by-obj
  [file-path attr op value]
  (-> (IRODSGenQueryBuilder. true nil)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_META_DATA_ATTR_NAME)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_META_DATA_ATTR_VALUE)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_META_DATA_ATTR_UNITS)
      #_(.addSelectAsGenQueryValue RodsGenQueryEnum/COL_COLL_NAME)
      #_(.addSelectAsGenQueryValue RodsGenQueryEnum/COL_DATA_NAME)
      (.addConditionAsGenQueryField RodsGenQueryEnum/COL_META_DATA_ATTR_NAME
                                    QueryConditionOperators/EQUAL attr)
      (.addConditionAsGenQueryField RodsGenQueryEnum/COL_META_DATA_ATTR_VALUE
                                    (op->constant op)
                                    (str value))
      (.exportIRODSQueryFromBuilder 50000)))

(defn list-files-with-attr
  [cm attr]
  (let [query (build-file-attr-query attr)
        rs    (.executeIRODSQueryAndCloseResult (:executor cm) query 0)]
    (map #(string/join "/" (.getColumnsAsList %)) (.getResults rs))))

(defn list-files-with-avu
  [cm name op value]
  (let [query    (build-file-avu-query name op value)
        rs       (.executeIRODSQueryAndCloseResult (:executor cm) query 0)]
    (map #(string/join "/" (.getColumnsAsList %)) (.getResults rs))))

(def ^:private file-avu-query-columns
  {:name  RodsGenQueryEnum/COL_META_DATA_ATTR_NAME
   :value RodsGenQueryEnum/COL_META_DATA_ATTR_VALUE
   :unit  RodsGenQueryEnum/COL_META_DATA_ATTR_UNITS})

(def ^:private dir-avu-query-columns
  {:name  RodsGenQueryEnum/COL_META_COLL_ATTR_NAME
   :value RodsGenQueryEnum/COL_META_COLL_ATTR_VALUE
   :unit  RodsGenQueryEnum/COL_META_COLL_ATTR_UNITS})

(defn- add-conditions-from-avu-spec
  "Adds conditions from an AVU specification to a general query builder. The query specification
   is a map in the following format:

       {:name  \"name\"
        :value \"value\"
        :unit  \"unit\"}

   The values in the map are strings indicating the name, value or unit of the AVUs to match. Each
   entry in the map is optional, so that the caller can search for any combination of name and
   value. For example, to search for AVUs named 'foo', the AVU specification would simply be
   {:name \"foo\"}. Unrecognized keys in the AVU specification are currently ignored and conditions
   are not added for null values."
  [cols builder avu-spec]
  (->> (remove (comp nil? last) avu-spec)
       (map (fn [[k v]] [(cols k) v]))
       (remove (comp nil? first))
       (map
        (fn [[col v]]
          (.addConditionAsGenQueryField builder col QueryConditionOperators/EQUAL v)))
       (dorun)))

(defn- build-subtree-query-from-avu-spec
  "Builds a subtree query from a path and an AVU specification.  The AVU specification is a map
   in the following format:

       {:name  \"name\"
        :value \"value\"
        :unit  \"unit\"}

   The values in the map are strings indicating the name, value or unit of the AVUs to match. Each
   entry in the map is optional, so that the caller can search for any combination of name and
   value. For example, to search for AVUs named 'foo', the AVU specification would simply be
   {:name \"foo\"}. Unrecognized keys in the AVU specification are currently ignored and conditions
   are not added for null values.

   The path is the absolute path to the root of the subtree to search. Items that are not in this
   directory or any of its descendants will not be matched. The root of the subtree is included
   in the search."
  [select-columns condition-columns path avu-spec]
  (let [builder (IRODSGenQueryBuilder. true nil)]
    (dorun (map #(.addSelectAsGenQueryValue builder %) select-columns))
    (when path
      (.addConditionAsGenQueryField builder
                                    RodsGenQueryEnum/COL_COLL_NAME
                                    QueryConditionOperators/LIKE
                                    (str path \%)))
    (add-conditions-from-avu-spec condition-columns builder avu-spec)
    (.exportIRODSQueryFromBuilder builder 50000)))

(defn- list-items-in-tree-with-attr
  "Lists either files or directories in a subtree given the path to the root of the subtree and an
   AVU specification. The AVU specification is a map in the following format:

       {:name  \"name\"
        :value \"value\"
        :unit  \"unit\"}

   The values in the map are strings indicating the name, value or unit of the AVUs to match. Each
   entry in the map is optional, so that the caller can search for any combination of name and
   value. For example, to search for AVUs named 'foo', the AVU specification would simply be
   {:name \"foo\"}. Unrecognized keys in the AVU specification are currently ignored and conditions
   are not added for null values.

   The path is the absolute path to the root of the subtree to search. Items that are not in this
   directory or any of its descendants will not be matched. The root of the subtree is included
   in the search.

   The select-columns parameter indicates which columns should be selected from the query.  The
   condition-columns parameter is a map indicating which constants to use in the query for the
   :name, :value, and :unit elements of the AVU specification.  The format-row parameter is a
   function that can be used to format each row in the result set.  The single parameter to this
   function is an instance of IRODSQueryResultRow."
  [select-columns condition-columns format-row cm path avu-spec]
  (let [query (build-subtree-query-from-avu-spec select-columns condition-columns path avu-spec)]
    (->> (.executeIRODSQueryAndCloseResult (:executor cm) query 0)
         (.getResults)
         (mapv format-row))))

(def list-files-in-tree-with-attr
  "Lists the paths to files in a subtree given the path to the root of the subtree and an AVU
   specification. The AVU specification is a map in the following format:

       {:name  \"name\"
        :value \"value\"
        :unit  \"unit\"}

   The values in the map are strings indicating the name, value or unit of the AVUs to match. Each
   entry in the map is optional, so that the caller can search for any combination of name and
   value. For example, to search for AVUs named 'foo', the AVU specification would simply be
   {:name \"foo\"}. Unrecognized keys in the AVU specification are currently ignored and conditions
   are not added for null values.

   The path is the absolute path to the root of the subtree to search. Items that are not in this
   directory or any of its descendants will not be matched. The root of the subtree is included
   in the search."
  (partial list-items-in-tree-with-attr
           [RodsGenQueryEnum/COL_COLL_NAME RodsGenQueryEnum/COL_DATA_NAME]
           file-avu-query-columns
           #(string/join "/" (.getColumnsAsList %))))

(def list-collections-in-tree-with-attr
  "Lists the paths to directories in a subtree given the path to the root of the subtree and an
   AVU specification. The AVU specification is a map in the following format:

       {:name  \"name\"
        :value \"value\"
        :unit  \"unit\"}

   The values in the map are strings indicating the name, value or unit of the AVUs to match. Each
   entry in the map is optional, so that the caller can search for any combination of name and
   value. For example, to search for AVUs named 'foo', the AVU specification would simply be
   {:name \"foo\"}. Unrecognized keys in the AVU specification are currently ignored and conditions
   are not added for null values.

   The path is the absolute path to the root of the subtree to search. Items that are not in this
   directory or any of its descendants will not be matched. The root of the subtree is included
   in the search."
  (partial list-items-in-tree-with-attr
           [RodsGenQueryEnum/COL_COLL_NAME]
           dir-avu-query-columns
           #(str (first (.getColumnsAsList %)))))

(defn list-everything-in-tree-with-attr
  [cm path avu-spec]
  "Lists the paths to both files and directories in a subtree given the path to the root of the
   subtree and an AVU specification. The AVU specification is a map in the following format:

       {:name  \"name\"
        :value \"value\"
        :unit  \"unit\"}

   The values in the map are strings indicating the name, value or unit of the AVUs to match. Each
   entry in the map is optional, so that the caller can search for any combination of name and
   value. For example, to search for AVUs named 'foo', the AVU specification would simply be
   {:name \"foo\"}. Unrecognized keys in the AVU specification are currently ignored and conditions
   are not added for null values.

   The path is the absolute path to the root of the subtree to search. Items that are not in this
   directory or any of its descendants will not be matched. The root of the subtree is included
   in the search."
  (doall (mapcat #(% cm path avu-spec)
                 [list-collections-in-tree-with-attr list-files-in-tree-with-attr])))

(defn get-avus-by-collection
  "Returns AVUs associated with a collection that have the given attribute and value."
  [cm file-path attr units]
  (let [query [(AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/UNITS
                AVUQueryOperatorEnum/EQUAL
                units)
               (AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/ATTRIBUTE
                AVUQueryOperatorEnum/EQUAL
                attr)]]
    (mapv
     #(hash-map
       :attr  (.getAvuAttribute %1)
       :value (.getAvuValue %1)
       :unit  (.getAvuUnit %1))
     (.findMetadataValuesByMetadataQueryForCollection (:collectionAO cm) query file-path))))

(defn list-collections-with-attr-units
  [cm attr units]
  (let [query [(AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/UNITS
                AVUQueryOperatorEnum/EQUAL
                units)
               (AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/ATTRIBUTE
                AVUQueryOperatorEnum/EQUAL
                attr)]]
    (mapv
     #(.getCollectionName %)
     (.findDomainByMetadataQuery (:collectionAO cm) query))))

(defn list-collections-with-attr-value
  [cm attr value]
  (let [query [(AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/VALUE
                AVUQueryOperatorEnum/EQUAL
                (str value))
               (AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/ATTRIBUTE
                AVUQueryOperatorEnum/EQUAL
                attr)]]
    (mapv
     #(.getCollectionName %)
     (.findDomainByMetadataQuery (:collectionAO cm) query))))

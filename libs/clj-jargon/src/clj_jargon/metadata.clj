(ns clj-jargon.metadata
  (:use [clj-jargon.validations]
        [clj-jargon.item-info :only [is-dir?]])
  (:require [clojure.string :as string]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure-commons.error-codes :refer [ERR_NOT_WRITEABLE]])
  (:import [org.irods.jargon.core.exception CatNoAccessException]
           [org.irods.jargon.core.pub DataObjectAO
                                      CollectionAO
                                      IRODSGenQueryExecutor]
           [org.irods.jargon.core.pub.domain AvuData
                                             Collection]
           [org.irods.jargon.core.query IRODSGenQueryBuilder
                                        IRODSQueryResultRow
                                        QueryConditionOperators
                                        RodsGenQueryEnum
                                        AVUQueryElement
                                        AVUQueryElement$AVUQueryPart
                                        AVUQueryOperatorEnum
                                        MetaDataAndDomainData]))

(defn map2avu
  "Converts an avu map into an AvuData instance."
  [avu-map]
  (AvuData/instance (:attr avu-map) (:value avu-map) (:unit avu-map)))

(defn- avu2map
  [^MetaDataAndDomainData avu]
  (hash-map :attr  (.getAvuAttribute avu)
            :value (.getAvuValue avu)
            :unit  (.getAvuUnit avu)))

(defn get-metadata
  "Returns all of the metadata associated with a path."
  [{^DataObjectAO data-ao :dataObjectAO
    ^CollectionAO collection-ao :collectionAO
    :as cm}
   ^String dir-path]
  (validate-path-lengths dir-path)
  (mapv avu2map
    (if (is-dir? cm dir-path)
      (.findMetadataValuesForCollection collection-ao dir-path)
      (.findMetadataValuesForDataObject data-ao dir-path))))

(defn get-attribute
  "Returns a list of avu maps for set of attributes associated with dir-path"
  [cm dir-path attr]
  (validate-path-lengths dir-path)
  (filter
    #(= (:attr %1) attr)
    (get-metadata cm dir-path)))

(defn get-attributes
  "Returns a list of avu maps for a set of attributes associated with dir-path."
  [cm attrs path]
  (validate-path-lengths path)
  (filter
    #(contains? attrs (:attr %1))
    (get-metadata cm path)))

(defn get-attribute-value
  [cm apath attr val]
  (validate-path-lengths apath)
  (filter
    #(and (= (:attr %1) attr)
          (= (:value %1) val))
    (get-metadata cm apath)))

(defn attribute?
  "Returns true if the path has the associated attribute."
  [cm dir-path attr]
  (validate-path-lengths dir-path)
  (pos? (count (get-attribute cm dir-path attr))))

(defn attr-value?
  "Returns a truthy value if path has metadata that has an attribute of attr and
   a value of val."
  ([cm path attr val]
    (attr-value? (get-metadata cm path) attr val))
  ([metadata attr val]
    (-> (filter
          #(and (= (:attr %1) attr)
                (= (:value %1) val))
          metadata)
      count
      pos?)))

(defmulti add-avu
          (fn [ao-obj dir-path avu] (type ao-obj)))
(defmethod add-avu CollectionAO
  [^CollectionAO ao-obj ^String dir-path ^AvuData avu]
  (.addAVUMetadata ao-obj dir-path avu))
(defmethod add-avu DataObjectAO
  [^DataObjectAO ao-obj ^String dir-path ^AvuData avu]
  (.addAVUMetadata ao-obj dir-path avu))

(defmulti modify-avu
          (fn [ao-obj dir-path old-avu avu] (type ao-obj)))
(defmethod modify-avu CollectionAO
  [^CollectionAO ao-obj ^String dir-path ^AvuData old-avu ^AvuData avu]
  (.modifyAVUMetadata ao-obj dir-path old-avu avu))
(defmethod modify-avu DataObjectAO
  [^DataObjectAO ao-obj ^String dir-path ^AvuData old-avu ^AvuData avu]
  (.modifyAVUMetadata ao-obj dir-path old-avu avu))

(defmulti delete-avu
          (fn [ao-obj dir-path avu] (type ao-obj)))
(defmethod delete-avu CollectionAO
  [^CollectionAO ao-obj ^String dir-path ^AvuData avu]
  (.deleteAVUMetadata ao-obj dir-path avu))
(defmethod delete-avu DataObjectAO
  [^DataObjectAO ao-obj ^String dir-path ^AvuData avu]
  (.deleteAVUMetadata ao-obj dir-path avu))

(defn add-metadata
  [cm dir-path attr value unit]
  (validate-path-lengths dir-path)
  (try+
    (let [ao-obj (if (is-dir? cm dir-path)
                     (:collectionAO cm)
                     (:dataObjectAO cm))]
      (add-avu ao-obj dir-path (AvuData/instance attr value unit)))
    (catch CatNoAccessException _
      (throw+ {:error_code ERR_NOT_WRITEABLE :path dir-path}))))


(defn set-metadata
  "Sets an avu for dir-path."
  [cm dir-path attr value unit]
  (validate-path-lengths dir-path)
  (let [avu    (AvuData/instance attr value unit)
        ao-obj (if (is-dir? cm dir-path)
                 (:collectionAO cm)
                 (:dataObjectAO cm))]
    (if (zero? (count (get-attribute cm dir-path attr)))
      (add-avu ao-obj dir-path avu)
      (let [old-avu (map2avu (first (get-attribute cm dir-path attr)))]
        (modify-avu ao-obj dir-path old-avu avu)))))

(defn- delete-meta
  [cm dir-path attr-func]
  (validate-path-lengths dir-path)
  (let [fattr  (first (attr-func))
        avu    (map2avu fattr)
        ao-obj (if (is-dir? cm dir-path)
                   (:collectionAO cm)
                   (:dataObjectAO cm))]
    (delete-avu ao-obj dir-path avu)))

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
        (delete-avu ao dir-path (map2avu avu-map))))))

(defn- ^QueryConditionOperators op->constant
  [op]
  (or ({:between         QueryConditionOperators/BETWEEN
        :=               QueryConditionOperators/EQUAL
        :>               QueryConditionOperators/GREATER_THAN
        :>=              QueryConditionOperators/GREATER_THAN_OR_EQUAL_TO
        :in              QueryConditionOperators/IN
        :<               QueryConditionOperators/LESS_THAN
        :<=              QueryConditionOperators/LESS_THAN_OR_EQUAL_TO
        :like            QueryConditionOperators/LIKE
        :not=            QueryConditionOperators/NOT_EQUAL
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
  [^String name op value]
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
  [^String name]
  (-> (IRODSGenQueryBuilder. true nil)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_COLL_NAME)
      (.addSelectAsGenQueryValue RodsGenQueryEnum/COL_DATA_NAME)
      (.addConditionAsGenQueryField RodsGenQueryEnum/COL_META_DATA_ATTR_NAME
                                    QueryConditionOperators/EQUAL name)
      (.exportIRODSQueryFromBuilder 50000)))


(defn- format-result
  [^IRODSQueryResultRow rr]
  (string/join "/" (.getColumnsAsList rr)))

(defn list-files-with-attr
  [{^IRODSGenQueryExecutor executor :executor} attr]
  (let [query (build-file-attr-query attr)
        rs    (.executeIRODSQueryAndCloseResult executor query 0)]
    (map format-result (.getResults rs))))

(defn list-files-with-avu
  [{^IRODSGenQueryExecutor executor :executor} name op value]
  (let [query    (build-file-avu-query name op value)
        rs       (.executeIRODSQueryAndCloseResult executor query 0)]
    (map format-result (.getResults rs))))

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
  [cols ^IRODSGenQueryBuilder builder avu-spec]
  (->> (remove (comp nil? last) avu-spec)
       (map (fn [[k v]] [(cols k) v]))
       (remove (comp nil? first))
       (map
        (fn [[^RodsGenQueryEnum col ^String v]]
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
  [select-columns condition-columns format-row {^IRODSGenQueryExecutor executor :executor} path avu-spec]
  (let [query (build-subtree-query-from-avu-spec select-columns condition-columns path avu-spec)]
    (->> (.executeIRODSQueryAndCloseResult executor query 0)
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
           format-result))

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
           (fn [^IRODSQueryResultRow rr] (str (first (.getColumnsAsList rr))))))

(defn list-everything-in-tree-with-attr
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
  [cm path avu-spec]
  (doall (mapcat #(% cm path avu-spec)
                 [list-collections-in-tree-with-attr list-files-in-tree-with-attr])))

(defn get-avus-by-collection
  "Returns AVUs associated with a collection that have the given attribute and value."
  [{^CollectionAO collection-ao :collectionAO} file-path attr units]
  (let [query [(AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/UNITS
                AVUQueryOperatorEnum/EQUAL
                units)
               (AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/ATTRIBUTE
                AVUQueryOperatorEnum/EQUAL
                attr)]]
    (mapv avu2map
     (.findMetadataValuesByMetadataQueryForCollection collection-ao query file-path))))

(defn- get-coll-name
  [^Collection coll]
  (.getCollectionName coll))

(defn list-collections-with-attr-units
  [{^CollectionAO collection-ao :collectionAO} attr units]
  (let [query [(AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/UNITS
                AVUQueryOperatorEnum/EQUAL
                units)
               (AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/ATTRIBUTE
                AVUQueryOperatorEnum/EQUAL
                attr)]]
    (mapv get-coll-name
     (.findDomainByMetadataQuery collection-ao query))))

(defn list-collections-with-attr-value
  [{^CollectionAO collection-ao :collectionAO} attr value]
  (let [query [(AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/VALUE
                AVUQueryOperatorEnum/EQUAL
                (str value))
               (AVUQueryElement/instanceForValueQuery
                AVUQueryElement$AVUQueryPart/ATTRIBUTE
                AVUQueryOperatorEnum/EQUAL
                attr)]]
    (mapv get-coll-name
     (.findDomainByMetadataQuery collection-ao query))))


(defn list-everything-with-attr-value
  "Generates a sequence of all collections and data objects with a given attribute having a given
   value.

   Parameters:
     cm    - the connected jargon context
     attr  - the name of the attribute
     value - the value of the attribute

   Returns:
     It returns a sequence of collections and data object paths."
  [cm attr value]
  (concat (list-collections-with-attr-value cm attr value) (list-files-with-avu cm attr := value)))

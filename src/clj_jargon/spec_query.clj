(ns clj-jargon.spec-query
  (:import [org.irods.jargon.core.exception DataNotFoundException JargonException]
           [org.irods.jargon.core.pub.domain SpecificQueryDefinition]
           [org.irods.jargon.core.query SpecificQuery]))

(defn- get-query-ao
  "Gets the specific query AO for the current iRODS session."
  [cm]
  (.getSpecificQueryAO (:accessObjectFactory cm) (:irodsAccount cm)))

(defn- define-query
  "Defines a specific query in iRODS. This function ignores exceptions thrown by Jargon because
   Jargon will throw an exception if the query already exists."
  [query-ao [alias query]]
  (try
    (.addSpecificQuery query-ao (SpecificQueryDefinition. alias query))
    (catch JargonException _)))

(defn define-specific-queries
  "Defines the specific queries in a map indexed by query alias."
  [cm queries]
  (dorun (map (partial define-query (get-query-ao cm)) queries)))

(defn- delete-query
  "Deletes a specific query in iRODS."
  [query-ao [alias query]]
  (.removeSpecificQuery query-ao (SpecificQueryDefinition. alias query)))

(defn delete-specific-queries
  "Deletes the queries in a map indexed by query alias."
  [cm queries]
  (dorun (map (partial delete-query (get-query-ao cm))  queries)))

(defn query-specifically
  [cm query-ao query-inst limit]
  (try
    (->> (.executeSpecificQueryUsingAlias query-ao query-inst limit)
      (.getResults)
      (map #(vec (.getColumnsAsList %))))
    (catch DataNotFoundException _ [])))

(defn- get-specific-query-page
  [cm alias offset limit args]
  (let [query-ao  (get-query-ao cm)
        args      (conj (vec (or args [])) (str limit) (str offset))
        query     (SpecificQuery/instanceArguments alias args 0 "")]
    (query-specifically cm query-ao query limit)))

(defn get-specific-query-results
  [cm alias & args]
  (let [query-ao (get-query-ao cm)
        args     (vec (or args []))
        query (SpecificQuery/instanceArguments alias args 0 "")]
    (query-specifically cm query-ao query 1000000)))

(defn specific-query?
  [cm alias]
  (try
    (.findSpecificQueryByAlias (get-query-ao cm) alias)
    true
    (catch Exception _ false)))

(defn paged-query
  [cm alias limit offset & args]
  (let [page (get-specific-query-page cm alias offset limit args)]
    (if (seq page) page [])))

(defn execute-specific-query
  [cm alias page-size & args]
  (letfn [(get-seq [offset]
            (let [page (get-specific-query-page cm alias offset page-size args)]
              (if (seq page)
                (lazy-cat page (get-seq (+ offset page-size)))
                [])))]
    (get-seq 0)))

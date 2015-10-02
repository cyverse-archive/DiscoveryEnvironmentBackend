(ns clj-jargon.lazy-listings
  (:require [clj-jargon.spec-query :as sq]
            [clojure-commons.file-utils :as ft])
  (:import  [org.irods.jargon.core.pub.domain IRODSDomainObject]
            [org.irods.jargon.core.pub CollectionAndDataObjectListAndSearchAO]))

(defn user-collection-perms
  "Lists the users and their permissions that have access to a collection."
  [cm collection]
  (sq/execute-specific-query
    cm "IPCUserCollectionPerms" 50000 (ft/dirname collection) collection))

(defn user-dataobject-perms
  "Lists the user and their permissions that have access to a dataobject."
  [cm dataobject-path]
  (sq/execute-specific-query 
    cm "IPCUserDataObjectPerms" 50000 (ft/dirname dataobject-path) (ft/basename dataobject-path)))

(defn- get-next-offset
  "Gets the next offset from a page of results, returning zero if the page is the last page in
   the result set."
  [page]
  (let [^IRODSDomainObject entry (last page)]
    (if-not (or (empty? page) (.isLastResult entry))
      (.getCount entry)
      0)))

(defn- lazy-listing
  "Produces a lazy listing of a set of data objects or collections, given a function that can
   be used to obtain the next page from the result set."
  [get-page]
  (letfn [(get-seq [offset]
            (let [page        (get-page offset)
                  next-offset (get-next-offset page)]
              (if (pos? next-offset)
                (lazy-cat page (get-seq next-offset))
                page)))]
    (get-seq 0)))

(defn list-subdirs-in
  "Returns a lazy listing of the subdirectories in the directory at the given path."
  [{^CollectionAndDataObjectListAndSearchAO lister :lister} dir-path]
  (lazy-listing #(.listCollectionsUnderPathWithPermissions lister dir-path %)))

(defn list-files-in
  "Returns a lazy listing of the files in the directory at the given path."
  [{^CollectionAndDataObjectListAndSearchAO lister :lister} dir-path]
  (lazy-listing #(.listDataObjectsUnderPathWithPermissions lister dir-path %)))

(defn paged-list-entries
  "Returns a paged directory listing."
  [cm user dir-path sort-col sort-order limit offset]
  (cond
    (and (= sort-col "ID") (= sort-order "ASC"))
    (sq/paged-query cm "IPCEntryListingPathSortASC" limit offset user dir-path)
    
    (and (= sort-col "ID") (= sort-order "DESC"))
    (sq/paged-query cm "IPCEntryListingPathSortDESC" limit offset user dir-path)
    
    (and (= sort-col "NAME") (= sort-order "ASC"))
    (sq/paged-query cm "IPCEntryListingNameSortASC" limit offset user dir-path)
    
    (and (= sort-col "NAME") (= sort-order "DESC"))
    (sq/paged-query cm "IPCEntryListingNameSortDESC" limit offset user dir-path)
    
    (and (= sort-col "SIZE") (= sort-order "ASC"))
    (sq/paged-query cm "IPCEntryListingSizeSortASC" limit offset user dir-path)
    
    (and (= sort-col "SIZE") (= sort-order "DESC"))
    (sq/paged-query cm "IPCEntryListingSizeSortDESC" limit offset user dir-path)
    
    (and (= sort-col "DATECREATED") (= sort-order "ASC"))
    (sq/paged-query cm "IPCEntryListingCreatedSortASC" limit offset user dir-path)
    
    (and (= sort-col "DATECREATED") (= sort-order "DESC"))
    (sq/paged-query cm "IPCEntryListingCreatedSortDESC" limit offset user dir-path)
    
    (and (= sort-col "LASTMODIFIED") (= sort-order "ASC"))
    (sq/paged-query cm "IPCEntryListingLastModSortASC" limit offset user dir-path)
    
    (and (= sort-col "LASTMODIFIED") (= sort-order "DESC"))
    (sq/paged-query cm "IPCEntryListingLastModSortDESC" limit offset user dir-path)
    
    :else
    (sq/paged-query cm "IPCEntryListingNameSortASC" limit offset user dir-path)))

(defn list-collections-under-path
  "Lists all of the collections under a path."
  [cm user dir-path]
  (sq/get-specific-query-results cm "IPCListCollectionsUnderPath" user dir-path))

(defn count-list-entries
  "Returns the number of entries in a directory listing. Useful for paging."
  [cm user dir-path]
  (-> (sq/get-specific-query-results cm "IPCCountDataObjectsAndCollections" user dir-path)
    (first)
    (first)
    (Integer/parseInt)))

(defn num-collections-under-path
  "The number of collections directly under the specified path. In other words, 
   it's not recursive."
  [cm user dir-path]
  (-> (sq/get-specific-query-results cm "IPCCountCollectionsUnderPath" user dir-path)
    (first)
    (first)
    (Integer/parseInt)))

(defn num-dataobjects-under-path
  "The number of collections directly under the specified path. In other words, 
   it's not recursive."
  [cm user dir-path]
  (-> (sq/get-specific-query-results cm "IPCCountDataObjectsUnderPath" user dir-path)
    (first)
    (first)
    (Integer/parseInt)))


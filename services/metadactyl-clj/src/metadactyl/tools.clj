(ns metadactyl.tools
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.queries]
        [kameleon.util.search]
        [metadactyl.containers :only [add-tool-container]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [metadactyl.validation :only [verify-tool-name-location]]
        [clojure.string :only [upper-case]]
        [korma.core :exclude [update]]
        [korma.db :only [transaction]])
  (:require [metadactyl.persistence.app-metadata :as persistence]))

(defn- add-search-where-clauses
  "Adds where clauses to a base tool search query to restrict results to tools that contain the
   given search term in their name or description."
  [base-query search-term]
  (if search-term
    (let [search-term (format-query-wildcards search-term)
          search-term (str "%" search-term "%")]
      (where base-query
             (or
              {(sqlfn lower :tools.name) [like (sqlfn lower search-term)]}
              {(sqlfn lower :tools.description) [like (sqlfn lower search-term)]})))
    base-query))

(defn- add-hidden-tool-types-clause
  "Adds the clause used to filter out hidden tool types if hidden tool types are not supposed to
   be included in the result set."
  [base-query include-hidden]
  (if-not include-hidden
    (where base-query {:tool_types.hidden false})
    base-query))

(defn tool-listing-base-query
  "Obtains a listing query for tools, with optional search and paging params."
  ([]
   (-> (select* tools)
       (fields [:tools.id :id]
               [:tools.name :name]
               [:tools.description :description]
               [:tools.location :location]
               [:tool_types.name :type]
               [:tools.version :version]
               [:tools.attribution :attribution])
       (join tool_types)))
  ([{search-term :search :keys [sort-field sort-dir limit offset include-hidden]
                         :or {include-hidden false}}]
   (let [sort-field (when sort-field (keyword (str "tools." sort-field)))
         sort-dir (when sort-dir (keyword (upper-case sort-dir)))]
     (-> (tool-listing-base-query)
         (add-search-where-clauses search-term)
         (add-query-sorting sort-field sort-dir)
         (add-query-limit limit)
         (add-query-offset offset)
         (add-hidden-tool-types-clause include-hidden)))))

(defn search-tools
  "Obtains a listing of tools for the tool search service."
  [params]
  {:tools
     (map remove-nil-vals
       (select (tool-listing-base-query params)))})

(defn get-tool
  "Obtains a tool by ID."
  [tool-id]
  (->> (first (select (tool-listing-base-query) (where {:tools.id tool-id})))
       (assert-not-nil [:tool-id tool-id])
       remove-nil-vals))

(defn get-tools-by-id
  "Obtains a listing of tools for the given list of IDs."
  [tool-ids]
  (map remove-nil-vals
    (select (tool-listing-base-query) (where {:tools.id [in tool-ids]}))))

(defn- add-new-tool
  [{:keys [container] :as tool}]
  (verify-tool-name-location tool)
  (let [tool-id (persistence/add-tool tool)]
    (when container
      (add-tool-container tool-id container))
    tool-id))

(defn add-tools
  "Adds a list of tools to the database, returning a list of IDs of the tools added."
  [{:keys [tools]}]
  (transaction
    (let [tool-ids (doall (map add-new-tool tools))]
      {:tool_ids tool-ids})))

(defn update-tool
  [{:keys [id] :as tool}]
  (persistence/update-tool tool)
  (get-tool id))

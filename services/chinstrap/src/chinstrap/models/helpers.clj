(ns chinstrap.models.helpers
  (:use [monger.operators :only [$nin]])
  (:require [clojure.string :as string]
            [monger.collection :as mc]))

(def ^:private completed-states ["Failed" "Completed"])

(defn format-data-for-graph
  "This function takes in dates and their counts and parses them into a JSON
  object for easy graph data parsing in javascript."
  [data]
  (map
    #(hash-map
      :date (key %)
      :count (val %))
    data))

(defn get-app-names
  "This function returns the names of apps with the passed status.
  E.G. (get-apps-names \"Completed\")"
  [status]
    (apply str
      (map #(str (:name (:state %)) "<br>")
        (mc/find-maps "jobs" {:state.status (str status)} [:state.name]))))

(defn- submap
  "Returns a map containing only the specified fields in the parent map."
  [m fields]
  (into {} (filter (comp fields first) m)))

(defn field-selector-for
  "Returns a monger field selector for a set of keywords representing fields
   in the state subobject of a job status record from the OSM."
  [ks]
  (->> ks
       (map name)
       (map #(str "state." %))
       (map keyword)
       vec))

(defn app-details
  "Returns information about apps in a particular state.  The set of fields
   to return is specified by the sequence in the first argument."
  [result-fields status]
  (map :state
       (mc/find-maps "jobs"
                     {:state.status status}
                     (field-selector-for result-fields))))

(defn app-details-str
  "Returns a string containing app details.  The set of fields to include in
   the string is specified by the sequence in the first argument."
  [result-fields status]
  (letfn [(fmt [data] (string/join " - " (map data result-fields)))]
    (map #(str (fmt %) "<br>") (app-details (set result-fields) status))))

(defn app-details-table
  "Returns an HTML table containing app details.  The set of fields to include
   in the table is specified by the sequence in the first argument."
  [result-fields status]
  (letfn [(tr   [f data]      (str "<tr>" (apply str (map f data)) "</tr>"))
          (htr  [fields]      (tr #(str "<th>" (name %) "</th>") fields))
          (dtr  [ks data]     (tr #(str "<td>" (data %) "</td>") ks))
          (dtrs [ks data-seq] (apply str (map #(dtr ks %) data-seq)))]
    (str "<table><thead>" (htr result-fields) "</thead><tbody>"
         (dtrs result-fields (app-details (set result-fields) status))
         "</tbody></table>")))

(defn pending-analyses
  "Returns analyses that are not in a completed status.  If a grouping field
   is provided then the results will be grouped by that field.  If a set of
   result fields is included then the job information will only include those
   fields in addition to the grouping field."
  ([]
     (map :state
          (mc/find-maps "jobs" {:state.status {$nin completed-states}})))
  ([grouping-field]
     (group-by grouping-field (pending-analyses)))
  ([grouping-field result-fields]
     (let [all-fields (set (cons grouping-field result-fields))]
       (group-by grouping-field
                 (map #(submap % all-fields) (pending-analyses))))))

(defn fetch-submission-date-by-status
  "Helper fuction for graph data calls to the mongoDB, returns a map of the
   dates in milliseconds of apps with the passed status."
  ([]
   (map #(:submission_date (:state %))
    (mc/find-maps "jobs"
      {:state.status {"$in" ["Completed" "Failed"]}})))
  ([status]
  (map #(:submission_date (:state %))
    (mc/find-maps "jobs"
      {:state.status {"$in" [status]}}
      [:state.submission_date]))))

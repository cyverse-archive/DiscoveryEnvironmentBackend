(ns chinstrap.models.ajax-endpoints
  (:require [monger.collection :as mc]
            [noir.response :as nr]
            [chinstrap.models.sqlqueries :as cq]
            [clj-time.format :as format]
            [clj-time.coerce :as coerce])
  (:use [chinstrap.db :only [mongo-db]]
        [chinstrap.models.helpers]))

(def old-osm-format (format/formatter "EEE MMM d y H:m:s 'GMT'Z '(MST)'"))
(def old-osm-format-2 (format/formatter "y MMM d H:m:s"))
(def parser (format/formatter "MM yyyy"))

(defn get-valid-epoch
  "Returns a valid epoch timestamp."
  [date]
  (try (coerce/to-long (format/parse old-osm-format date))
    (catch IllegalArgumentException e
      (try (coerce/to-long (format/parse old-osm-format-2 date))
        (catch IllegalArgumentException e
          (Long/parseLong date))))))

(defn get-valid-datetime
  "Returns a valid epoch timestamp."
  [dt]
  (if (= (type dt) java.lang.Long)
    (Long/parseLong dt)
    (coerce/to-long (format/parse old-osm-format dt))))

;AJAX call from the Javascript file 'resources/public/js/day-graph.js' for graph data.
(defn get-day-data []
  (nr/json
    (format-data-for-graph
      (into (sorted-map) (reduce #(assoc %1 %2 (inc (%1 %2 0))) {}
        (map
          #(* 86400000 (long (/ (get-valid-epoch (str %)) 86400000)))
            (fetch-submission-date-by-status)))))))

;AJAX call from the Javascript file 'resources/public/js/month-graph.js' for graph data.
(defn get-month-data []
  (nr/json
    (format-data-for-graph
      (into (sorted-map) (reduce #(assoc %1 %2 (inc (%1 %2 0))) {}
        (map #(coerce/to-long (format/parse parser (format/unparse parser
                (coerce/from-long (get-valid-epoch (str %))))))
          (fetch-submission-date-by-status)))))))

;AJAX call from the Javascript file 'resources/public/js/day-graph.js' for graph data.
(defn get-day-data-for [status]
  (nr/json
    (format-data-for-graph
      (into (sorted-map) (reduce #(assoc %1 %2 (inc (%1 %2 0))) {}
        (map #(* 86400000 (long (/ (get-valid-epoch (str %)) 86400000)))
              (fetch-submission-date-by-status status)))))))
(def parser
  (format/formatter "MM yyyy"))

;AJAX call from the Javascript file 'resources/public/js/month-graph.js' for graph data.
(defn get-month-data-for [status]
  (nr/json
    (format-data-for-graph
      (into (sorted-map) (reduce #(assoc %1 %2 (inc (%1 %2 0))) {}
        (map #(coerce/to-long (format/parse parser (format/unparse parser
                (coerce/from-long (get-valid-epoch (str %))))))
          (fetch-submission-date-by-status status)))))))

;AJAX call from the Javascript file 'resources/public/js/get-info.js'.
(defn get-date-info [date]
  (nr/json {:tools
    (cq/count-apps
      (map #(str (:analysis_id (:state %)))
        (mc/find-maps @mongo-db "jobs" {:state.submission_date
          {"$gte" (read-string date) "$lt" (+ 86400000 (read-string date))}}
            [:state.analysis_id])))}))

;AJAX call from the Javascript file 'resources/public/js/get-integrators.js'.
(defn get-integrator-data-for [id]
  (nr/json {
      :data
        (cq/integrator-data id)
      :apps
        (cq/integrator-details id)}))

;AJAX call from the Javascript file 'resources/public/js/integrator-script.js'.
(defn get-integrator-data []
  (let [cq-data (map :count (cq/integrator-list))]
    (nr/json {
      :average
        (/ (reduce + cq-data) (count cq-data))
      :total
        (reduce + cq-data)
    })))

;AJAX call from the Javascript file 'resources/public/js/get-apps.js'.
(defn get-apps []
  (let [fmt-job-details (partial app-details-str [:user :name])]
    (nr/json {:running (mc/count @mongo-db "jobs" {:state.status "Running"}),
              :submitted (mc/count @mongo-db "jobs" {:state.status "Submitted"}),
              :failed (mc/count @mongo-db "jobs" {:state.status "Failed"}),
              :completed (mc/count @mongo-db "jobs" {:state.status "Completed"}),
              :running-names (fmt-job-details "Running"),
              :failed-names (fmt-job-details "Failed"),
              :submitted-names (fmt-job-details "Submitted")})))

;AJAX call to get information about pending analyses grouped by username.
(defn get-pending-analyses-by-user []
  (nr/json (pending-analyses :user #{:uuid})))

;AJAX call from the Javascript file 'resources/public/js/get-components.js'.
(defn get-components []
  (nr/json {:all (cq/all-app-count)
            :without (cq/unused-app-count)
            :with (cq/used-app-count)}))

;Historical data of app count
(defn get-historical-app-count []
  (nr/json {:count_by_bucket (cq/historical-app-count)
            :accumulated_count (cq/accumulated-app-count)}))

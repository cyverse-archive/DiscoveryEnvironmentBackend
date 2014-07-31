(ns jex.outgoing-xforms)

(defn filter-map
  [outgoing-map]
  (-> outgoing-map
    (assoc :status          "Submitted"
           :user            (:username outgoing-map)
           :output_manifest [])
    (dissoc :username
            :dag
            :final-output-job
            :steps
            :all-input-jobs
            :all-output-jobs
            :imkdir-job)))

(defn transform
  "Applies some transformations to the condor-map. Basically used
   to clean stuff up before dumping information into the OSM."
  [outgoing-map]
  (filter-map outgoing-map))

(ns facepalm.c180-2013062601
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130626.01")

(defn- remove-existing-compatibilities
  "Removes existing entries from the tool_type_property_type table."
  []
  (delete :tool_type_property_type))

(defn- update-compatibilities
  "Updates the tool_type_property_type table with the given set of values."
  [vals]
  (insert :tool_type_property_type
          (values vals)))

(defn- base-select
  "The base query used to retrieve property type compatibility information."
  []
  (-> (select* [:property_type :pt])
      (join [:tool_types :tt] {:pt.hid :pt.hid})
      (fields [:tt.id :tool_type_id] [:pt.hid :property_type_id])
      (order :pt.hid)))

(defn- update-compatibilities-for-condor
  "Updates the property type compatibilities for Condor jobs."
  []
  (update-compatibilities
   (-> (base-select)
       (where {:tt.name "executable"})
       (select))))

(defn- update-compatibilities-for-agave
  "Updates the property type compatibilities for Foundation API jobs."
  []
  (update-compatibilities
   (-> (base-select)
       (where {:tt.name "fAPI"
               :pt.name [not= "EnvironmentVariable"]})
       (select))))

(defn- update-property-type-compatibilities
  "Updates all property type compatibilities."
  []
  (println "\t* updating the property type compatibility table.")
  (remove-existing-compatibilities)
  (update-compatibilities-for-condor)
  (update-compatibilities-for-agave))

(defn convert
  "Performs the conversion for database version 1.8.0:20130626.01."
  []
  (println "Performing conversion for" version)
  (update-property-type-compatibilities))

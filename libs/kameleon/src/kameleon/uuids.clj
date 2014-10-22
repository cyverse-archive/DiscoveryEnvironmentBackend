(ns kameleon.uuids)

(defn uuid
  []
  (java.util.UUID/randomUUID))

(defmulti uuidify
  (fn [obj] (type obj)))

(defmethod uuidify java.util.UUID
  [uuid]
  uuid)

(defmethod uuidify java.lang.String
  [uuid]
  (java.util.UUID/fromString uuid))

(defmethod uuidify clojure.lang.Keyword
  [uuid]
  (uuidify (name uuid)))

(defmethod uuidify :default
  [uuid]
  nil)


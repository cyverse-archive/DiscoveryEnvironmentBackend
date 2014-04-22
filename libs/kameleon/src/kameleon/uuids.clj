(ns kameleon.uuids)

(defn uuid
  []
  (java.util.UUID/randomUUID))

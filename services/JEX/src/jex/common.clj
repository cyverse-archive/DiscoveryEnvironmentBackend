(ns jex.common)

(defn volumes-from-name
  [uuid name-prefix]
  (str name-prefix "-" uuid))

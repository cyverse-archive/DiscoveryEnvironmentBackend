(ns kameleon.misc-queries
  (:use [kameleon.core]
        [kameleon.entities]
        [korma.core]))

(defn user?
  "Checks to see if the username belongs to a real user."
  [username]
  (pos? (count (select users (where {:username username})))))

(defn user-id
  "Returns the user id associated with a username."
  [username]
  (-> (select users (where {:username username})) first :id))

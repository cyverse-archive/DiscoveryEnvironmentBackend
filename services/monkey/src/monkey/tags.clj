(ns monkey.tags
  "This namespace implements the ViewTags protocol for interacting with the tags data store through
   Korma."
  (:gen-class)
  (:require [clojure.java.jdbc :as jdbc]
            [monkey.props :as props])
  (:import [java.util UUID]
           [clojure.lang IFn ISeq PersistentArrayMap]))


(defprotocol ViewsTags
  "This protocol defines the read-only operations needed to interact with the tag database."

  (->>all-tags [_ ^ISeq ops]
    "This function takes a sequence of functions and starting with it threads a sequence of all of
     the tags in the database through the given functions.

     Parameters:
       ops - The sequence of functions to apply in order to the tags.")

  (^Integer count-tags [_]
    "Retreives a count of the number of tags in the database.")

  (^ISeq remove-missing [_ ^Iseq ids]
    "Filters a sequence of tags, removing only the tags that are not in the database.

     Parameters:
       ids - a sequence of tag Ids to filter

     Return:
       The filtered sequence.")

  (^ISeq tag-targets [_ ^UUID tag-id]
    "Given a certain tag, it retrieves all of the files and folders that have this tag.

     Parameters:
       tag-id - the tag's Id

     Returns:
       It returns a sequence of file and folder UUIDs."))


(deftype ^{:private true} Tags [db]
  ViewsTags

  (->>all-tags [_ ops]
    (let [consume (apply comp (reverse ops))]
      (jdbc/query db ["SELECT id, value, description, owner_id, created_on, modified_on FROM tags"]
        :result-set-fn #(dorun (consume %)))))

  (count-tags [_]
    (jdbc/query db ["SELECT COUNT(*) AS cnt FROM tags"]
      :result-set-fn (comp :cnt first)))

  (remove-missing [_ ids]
    (let [fmt-ids (apply str (interpose \, (map #(str \' % \') ids)))]
      (jdbc/query db [(str "SELECT id FROM tags WHERE id IN (" fmt-ids ")")]
        :row-fn :id)))

  (tag-targets [_ tag-id]
    (let [query "SELECT target_id, target_type
                   FROM attached_tags
                   WHERE tag_id = ? AND target_type IN ('file', 'folder')"]
      (jdbc/query db [query tag-id]))))


(defn with-tags
  "Opens a connection to the tags database, and calls the provided function, passing in a ViewsTags
   object it.

   Parameters:
     props - the configration properties
     op    - the function to call. It must accept a ViewsTags object as its only parameter.

   Returns:
     It returns whatever op returns."
  [^PersistentArrayMap props ^IFn op]
  (let [host    (props/tags-host props)
        port    (props/tags-port props)
        dbname  (props/tags-db props)
        subname (str "//" host ":" port "/" dbname)
        dbspec  {:classname   "org.postgresql.Driver"
                 :subprotocol "postgresql"
                 :subname     subname
                 :user        (props/tags-user props)
                 :password    (props/tags-password props)}]
    (jdbc/with-db-connection [conn dbspec]
      (op (->Tags conn)))))

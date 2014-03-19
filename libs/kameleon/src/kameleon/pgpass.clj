(ns kameleon.pgpass
  (:use [clojure.java.io :only [file reader]]
        [clojure.string :only [split]]))

(defn- entry-matches
  "Determines if an entry from the .pgpass file matches our search criteria."
  [search entry]
  (letfn [(matches [nm]
            (let [e (str (get entry nm))
                  s (get search nm)]
              (or (= e "*") (= e s))))]
    (every? matches [:host :port :db :user])))

(defn- line->entry
  "Converts a line from the .pgpass file to an entry map."
  [line]
  (->> (split line #":")
       (interleave [:host :port :db :user :password])
       (partition 2)
       (map vec)
       (into {})))

(defn- find-password
  "Searches for the password for our search criteria in the .pgpass file.
   Returns the matching password if one is found.  Returns nil otherwise."
  [in search]
  (let [entries (map line->entry (line-seq in))
        match (first (filter (partial entry-matches search) entries))]
    (if (nil? match) nil (:password match))))

(defn get-password
  "Obtains the database password from the user's .pgpass file for the given
   host, port, database and username.  Returns nil if the password can't be
   obtained."
  ([host port db user]
     (let [f (file (System/getProperty "user.home") ".pgpass")]
       (if (and (.isFile f) (.canRead f))
         (with-open [in (reader f)]
           (find-password in {:host host :port port :db db :user user}))
         nil)))
  ([in search]
     (find-password in search)))
